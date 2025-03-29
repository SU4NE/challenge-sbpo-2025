include .env

ifeq ($(OS),Windows_NT)
    TIMESTAMP := $(shell powershell -Command "Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'")
else
    TIMESTAMP := $(shell date +"%Y-%m-%d_%H-%M-%S")
endif

FINAL_FILE:=$(FINAL_FILE)_$(TIMESTAMP).txt

compile:
	@mvn -q clean package

run:
	python $(SOURCE_CODE_PATH) $(SOURCE_FOLDER) $(INPUT_DIR) $(OUTPUT_DIR)

java: compile
	java -jar target/ChallengeSBPO2025-1.0.jar $(INPUT_FILE) $(OUTPUT_FILE)

check:
	python $(SOURCE_CODE_CHECK_PATH) $(INPUT_FILE) $(OUTPUT_FILE)

check_all: compile
	@mkdir -p $(OUTPUT_DIR)
	@mkdir -p $(CHECK_DIR)
	@> $(FINAL_FILE)
	@for file in $(INPUT_DIR)/*.txt; do \
		start_time=$$(date +%s%N); \
		output_file=$(OUTPUT_DIR)/$$(basename $$file); \
		echo "$$file" >> $(FINAL_FILE); \
		if [ -n "$(MAX_RUNNING_TIME)" ]; then \
			timeout $(MAX_RUNNING_TIME) java -Xmx16g $(LIB_OPTION) -jar target/ChallengeSBPO2025-1.0.jar $$file $$output_file; \
		else \
			java -Xmx16g $(LIB_OPTION) -jar target/ChallengeSBPO2025-1.0.jar $$file $$output_file; \
		fi; \
		ret=$$?; \
		end_time=$$(date +%s%N); \
		elapsed_time=$$((end_time - start_time)); \
		if [ $$ret -ne 0 ]; then \
			echo "Execution failed for $$file" >> $(FINAL_FILE); \
		else \
			python $(SOURCE_CODE_CHECK_PATH) $$file $$output_file >> $(FINAL_FILE); \
		fi; \
		echo "Execution time: $$elapsed_time ns" >> $(FINAL_FILE); \
		if [ "$(CHECK_OUTPUT_SAVE)" = "True" ]; then \
			copy_date=$(TIMESTAMP); \
			mkdir -p $(CHECK_OUTPUT_DIR)/$$copy_date; \
			cp $$output_file $(CHECK_OUTPUT_DIR)/$$copy_date/; \
		fi; \
		echo "" >> $(FINAL_FILE); \
	done

perf_eval:
	python $(SOURCE_CODE_PERF_PATH) $(CHECK_DIR) $(OUTPUT_DIR_PERF) --plot $(ENABLE_PLOT_PERF)