# SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
#
# SPDX-License-Identifier: MIT

HEAD := $(shell git rev-parse --verify HEAD)
SKILL_FILE := .ai/skills/commit-or-pr-message/SKILL.md

define gh_head_run_id
	gh run list --workflow $(1) --commit $(HEAD) --json databaseId --jq '.[0].["databaseId"]'
endef

.PHONY: build
build:
	./gradlew build --console=plain

.PHONY: merge
merge: merge-head push
	@if gh pr view --json number > /dev/null 2>&1; then \
		# PR exists: run full CI first, then update PR message to save Copilot calls \
		$(MAKE) watch-full create-pr; \
	else \
		# No PR: create a minimal PR (no Copilot), run full CI, then generate message \
		$(MAKE) create-pr-minimal watch-full create-pr; \
	fi
	@$(MAKE) merge-squash

create-pr: build
	@tmp_dir=$$(mktemp -d); \
	head_before=$$(git rev-parse HEAD); \
	if gh pr view --json number > /dev/null 2>&1; then \
		printf '%s\n' "Updating PR message..."; \
		./scripts/pr-message.sh --title-file "$$tmp_dir/title.txt" --body-file "$$tmp_dir/body.txt" \
		  --skill-file "$(SKILL_FILE)" || exit 0; \
		head_after=$$(git rev-parse HEAD); \
		if [ "$$head_before" != "$$head_after" ]; then \
			./scripts/pr-message.sh --title-file "$$tmp_dir/title.txt" --body-file "$$tmp_dir/body.txt" \
			  --skill-file "$(SKILL_FILE)" || exit 0; \
		fi; \
		title=$$(cat "$$tmp_dir/title.txt"); \
		gh pr edit --title "$$title" --body-file "$$tmp_dir/body.txt" || exit 0; \
		GH_PAGER=cat gh pr view; \
	else \
		./scripts/pr-message.sh --title-file "$$tmp_dir/title.txt" --body-file "$$tmp_dir/body.txt" \
		  --skill-file "$(SKILL_FILE)" || exit 0; \
		head_after=$$(git rev-parse HEAD); \
		if [ "$$head_before" != "$$head_after" ]; then \
			./scripts/pr-message.sh --title-file "$$tmp_dir/title.txt" --body-file "$$tmp_dir/body.txt" \
			  --skill-file "$(SKILL_FILE)" || exit 0; \
		fi; \
		title=$$(cat "$$tmp_dir/title.txt"); \
		gh pr create --title "$$title" --body-file "$$tmp_dir/body.txt" || exit 0; \
		printf '%s\n' "PR created with generated message."; \
		GH_PAGER=cat gh pr view; \
	fi; \
	rm -rf "$$tmp_dir"

.PHONY: create-pr-minimal
# Create a minimal PR without invoking Copilot. Still requires local build success first.
create-pr-minimal: build
	@if gh pr view --json number > /dev/null 2>&1; then \
		printf '%s\n' "PR already exists; skipping minimal creation."; \
	else \
		title=$$(git log -1 --pretty=%s | head -n1); \
		[ -n "$$title" ] || title="chore: open PR"; \
		body="Temporary PR. Running full CI; description will be updated after success."; \
		gh pr create --title "$$title" --body "$$body" || exit 0; \
		printf '%s\n' "PR created (minimal)."; \
		GH_PAGER=cat gh pr view; \
	fi

push:
	git push

merge-head:
	git fetch --all --prune --prune-tags --tags --force
	git merge origin/HEAD

merge-squash:
	@if [ -n "$$({ git status --porcelain=1 2>/dev/null; } )" ]; then \
		printf '%s\n' "WARNING: Uncommitted changes detected. Review before merge." 1>&2; \
	fi; \
	printf '%s' "Proceed with squash merge? [Y/n] "; \
	read -r reply; \
	case "$$reply" in \
		""|y|Y|yes|YES) ;; \
		*) printf '%s\n' "Merge cancelled."; exit 1 ;; \
	esac; \
	gh pr merge --squash --delete-branch --auto

watch-full:
	@set -e; \
	  wf="full"; \
	  commit="$(HEAD)"; \
	  printf '%s\n' "Waiting for workflow '$$wf' run for commit $$commit..."; \
	  attempts=0; \
	  max_attempts=$${GH_WATCH_MAX_ATTEMPTS:-40}; \
	  sleep_seconds=$${GH_WATCH_SLEEP_SECONDS:-3}; \
	  run_id=""; \
	  while [ $$attempts -lt $$max_attempts ]; do \
	    run_id=$$(gh run list --workflow "$$wf" --commit "$$commit" --json databaseId --jq '.[0].databaseId // empty'); \
	    if [ -n "$$run_id" ]; then \
	      break; \
	    fi; \
	    attempts=$$((attempts+1)); \
	    sleep $$sleep_seconds; \
	  done; \
	  if [ -z "$$run_id" ]; then \
	    printf '%s\n' "ERROR: No '$$wf' run found for commit $$commit after $$((attempts*sleep_seconds)) seconds." 1>&2; \
	    printf '%s\n' "Hint: ensure the workflow triggers include this branch/PR event." 1>&2; \
	    exit 1; \
	  fi; \
	  printf '%s\n' "Found run ID $$run_id. Watching..."; \
	  gh run watch "$$run_id" --exit-status
