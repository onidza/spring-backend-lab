BASE_URL ?= http://localhost:8080
CACHE_MODE ?= NON_CACHE
USE_STAGES ?= false

K6 ?= k6
SCRIPT_GET_CLIENT_BY_ID ?= client/test-get-client.js

.PHONY: help client-get-by-id client-get-stages client-get-constant \
               cache-non cache-manual cache-spring

help:
	@echo "Targets:"
	@echo "  make client-get-by-id          # run with defaults"
	@echo "  make client-get-constant       # USE_STAGES=false"
	@echo "  make client-get-stages         # USE_STAGES=true"
	@echo ""
	@echo "  cache-non:        				# CACHE_MODE=NON_CACHE"
	@echo "  cache-manual:         			# CACHE_MODE=MANUAL"
	@echo "  cache-spring:         			# CACHE_MODE=SPRING"
	@echo ""
	@echo "Override vars:"
	@echo "  make client-get BASE_URL=... CACHE_MODE=... USE_STAGES=true"

client-get-by-id:
	@$(K6) run -e BASE_URL=$(BASE_URL) -e CACHE_MODE=$(CACHE_MODE) \
-e USE_STAGES=$(USE_STAGES) $(SCRIPT_GET_CLIENT_BY_ID)

client-get-constant:
	@$(MAKE) client-get-by-id USE_STAGES=false

client-get-stages:
	@$(MAKE) client-get-by-id USE_STAGES=true

cache-non:
	@$(MAKE) client-get-by-id CACHE_MODE=NON_CACHE

cache-manual:
	@$(MAKE) client-get-by-id CACHE_MODE=MANUAL

cache-spring:
	@$(MAKE) client-get-by-id CACHE_MODE=SPRING

