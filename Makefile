SHELL := /usr/bin/env bash

BOT  := broadcast

UNLTD_JCE_POLICY       := vendor/UnlimitedJCEPolicyJDK8
UNLTD_JCE_POLICY_JARS  := $(UNLTD_JCE_POLICY)/local_policy.jar $(UNLTD_JCE_POLICY)/US_export_policy.jar

default: package

all: package runtime build

package:
	mvn -Plinux package

build:
	docker build --tag wire/$(BOT) -f Dockerfile .

.PHONY: runtime
runtime: $(UNLTD_JCE_POLICY_JARS)
	docker build --tag wire/bots.runtime -f Dockerfile.runtime .

$(UNLTD_JCE_POLICY_JARS):
	$(MAKE) crypto_policy

.PHONY: crypto_policy
crypto_policy:
	@echo
	@echo "ERROR: Unlimited Strength Javarr Crypto Required"
	@echo
	@echo "Please download the strong cryptography policies for Java from"
	@echo "http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html"
	@echo "and unzip the archive into $(CURDIR)/vendor"
	@echo
	@exit 1

.PHONY: clean
clean: rm_dots
	mvn clean

install:
	mvn install

dist: install .metadata
	makedeb --name=$(NAME)          \
			--version=$(VERSION)    \
			--architecture=all      \
			--build=$(BUILD_NUMBER) \
			--debian-dir=deb        \
			--output-dir=target

.metadata:
	echo -e "NAME=$(NAME)\nVERSION=$(VERSION)\nBUILD_NUMBER=$(BUILD_NUMBER)\n" \
		> .metadata

rm_dots:
	-rm -r .metadata

run:
	java -jar target/$(BOT).jar server $(BOT).yaml