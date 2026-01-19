BASE_URL ?= http://localhost:8080
CACHE_MODE ?= NON_CACHE
USE_STAGES ?= true

K6 ?= k6

SCRIPT_GET_CLIENT_BY_ID ?= client/test-get-client-by-id.js
SCRIPT_GET_CLIENT_PAGE ?= client/test-get-client-page.js

SCRIPT_ADD_CLIENT ?= client/test-add-client.js
SCRIPT_UPDATE_CLIENT_BY_ID ?= client/test-update-by-id-client.js
SCRIPT_DELETE_CLIENT_BY_ID ?= client/test-delete-by-id-client.js

.PHONY: help run \
        client-get-by-id client-get-page \
        client-add, client-update, client-delete \
        cache-non cache-manual cache-spring \


help:
	@echo "k6 targets:"
	@echo "  make client-get-by-id        # run get-id scenario"
	@echo "  make client-get-page         # run page scenario"
	@echo "  make client-add         	  # run add scenario"
	@echo "  make client-update-by-id     # run update scenario"
	@echo "  make client-delete-by-id     # run delete scenario"
	@echo ""
	@echo "Examples (override vars):"
	@echo "  make client-get-by-id USE_STAGES=false"
	@echo "  make client-get-page  USE_STAGES=true"
	@echo "  make client-get-by-id CACHE_MODE=NON_CACHE"
	@echo "  make client-get-page  CACHE_MODE=SPRING"
	@echo "  make client-get-by-id BASE_URL=http://host.docker.internal:8080"
	@echo ""
	@echo "Cache shortcuts:"
	@echo "  make cache-non TARGET=client-get-by-id"
	@echo "  make cache-manual TARGET=client-get-page"
	@echo "  make cache-spring TARGET=client-get-by-id"
	@echo ""
	@echo "Vars:"
	@echo "  BASE_URL=$(BASE_URL)"
	@echo "  CACHE_MODE=$(CACHE_MODE)   (NON_CACHE|MANUAL|SPRING)"
	@echo "  USE_STAGES=$(USE_STAGES)   (true|false)"

# --- internal runner ---
# usage: make run SCRIPT=...
run:
	@$(K6) run \
		-e BASE_URL=$(BASE_URL) \
		-e CACHE_MODE=$(CACHE_MODE) \
		-e USE_STAGES=$(USE_STAGES) \
		$(SCRIPT)

# --- public targets ---
client-get-by-id:
	@$(MAKE) run SCRIPT=$(SCRIPT_GET_CLIENT_BY_ID)

client-get-page:
	@$(MAKE) run SCRIPT=$(SCRIPT_GET_CLIENT_PAGE)

client-add:
	@$(MAKE) run SCRIPT=$(SCRIPT_ADD_CLIENT)

client-update:
	@$(MAKE) run SCRIPT=$(SCRIPT_UPDATE_CLIENT_BY_ID)

client-delete:
	@$(MAKE) run SCRIPT=$(SCRIPT_DELETE_CLIENT_BY_ID)

# --- optional shortcuts ---
TARGET ?= client-get-by-id

cache-non:
	@$(MAKE) $(TARGET) CACHE_MODE=NON_CACHE

cache-manual:
	@$(MAKE) $(TARGET) CACHE_MODE=MANUAL

cache-spring:
	@$(MAKE) $(TARGET) CACHE_MODE=SPRING
