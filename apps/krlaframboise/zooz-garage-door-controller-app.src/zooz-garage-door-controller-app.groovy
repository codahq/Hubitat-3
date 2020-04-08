/*
 *  Zooz Garage Door Controller App v1.0	(Apps Code)
 *
 *
 * WARNING: Using a homemade garage door opener can be dangerous so use this code at your own risk. 
 *
 *  Changelog:
 *
 *    1.0 (04/08/2020)
 *      - Initial Release
 *
 *
 *	Copyright 2020 Kevin LaFramboise (@krlaframboise)
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of
 *  the License at:
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in
 *  writing, software distributed under the License is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 *  OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

import groovy.transform.Field

@Field static Map autoOffDelayOptions = [0:"Disabled", 2:"2 Seconds [DEFAULT]", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds"]

@Field static Map operatingDelayOptions = [0:"Disabled [DEFAULT]", 1:"1 Second", 2:"2 Seconds", 3:"3 Seconds", 4:"4 Seconds", 5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds", 8:"8 Seconds", 9:"9 Seconds", 10:"10 Seconds", 11:"11 Seconds", 12:"12 Seconds", 13:"13 Seconds", 14:"14 Seconds", 15:"15 Seconds", 16:"16 Seconds", 17:"17 Seconds", 18:"18 Seconds", 19:"19 Seconds", 20:"20 Seconds", 21:"21 Seconds", 22:"22 Seconds", 23:"23 Seconds", 24:"24 Seconds", 25:"25 Seconds", 26:"26 Seconds", 27:"27 Seconds", 28:"28 Seconds", 29:"29 Seconds", 30:"30 Seconds"]

@Field static Map operatingDurationOptions = [5:"5 Seconds", 6:"6 Seconds", 7:"7 Seconds", 8:"8 Seconds", 9:"9 Seconds", 10:"10 Seconds", 11:"11 Seconds", 12:"12 Seconds", 13:"13 Seconds", 14:"14 Seconds", 15:"15 Seconds [DEFAULT]", 16:"16 Seconds", 17:"17 Seconds", 18:"18 Seconds", 19:"19 Seconds", 20:"20 Seconds", 21:"21 Seconds", 22:"22 Seconds", 23:"23 Seconds", 24:"24 Seconds", 25:"25 Seconds", 26:"26 Seconds", 27:"27 Seconds", 28:"28 Seconds", 29:"29 Seconds", 30:"30 Seconds"]

@Field static Map enabledOptions = [0:"Disabled", 1:"Enabled [DEFAULT]"]

definition(
	name: "Zooz Garage Door Controller App",
    namespace: "krlaframboise",
    author: "Kevin LaFramboise",
    description: "DO NOT INSTALL, use the Zooz Garage Door Controller instead.",
	parent: "krlaframboise:Zooz Garage Door Controller",
	category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)


preferences {
	page(name:"pageMain")
	page(name:"pageRemove")
}

def pageMain() {
	dynamicPage(name: "pageMain", title: "", install: true, uninstall: false) {
		
		section("<b>Garage Door Name</b>") {
			paragraph "The Garage Door Name will be displayed in the Garage Door list when you open this SmartApp."

			paragraph "The child Door Control device created for this door will use this name followed by 'Opener'."
			
			label title: "Enter Garage Door Name:", required: true
			
			paragraph ""
		}
		
		section("<b>Relay Switch</b>") {
			paragraph "The Relay Switch will be used to activate the garage door."
			
			input "relaySwitch", "capability.switch",
				title: "Select Relay Switch:",
				required: true
			
			paragraph ""
		}
		
		section("<b>Open/Close Sensor</b>") {
			paragraph "The Open/Close Sensor will be used to determine the state of the garage door."
			
			input "contactSensor", "capability.contactSensor",
				title: "Select Open/Close Sensor:",
				required: true
			
			paragraph ""
		}
		
		section("<b>Relay Switch Auto-Off Timer</b>") {
			paragraph "The Auto-Off Timer will turn the relay off after a few seconds making it a momentary switch."
			
			paragraph "Leave this setting disabled if you already set the switch type on the ZEN16 to Garage Door Mode."
			
			input "autoOffDelay", "enum",
				title: "Select Auto-Off Timer:",
				required: false,
				defaultValue: autoOffDelaySetting,
				options: autoOffDelayOptions
			
			paragraph ""
		}
		
		section("<b>Garage Door Operating Delay</b>") {
			paragraph "The Operating Delay determines the amount of time it waits after changing the door control child device to OPENING/CLOSING before sending the on command to the Relay Switch."
			
			paragraph "This feature allows you to use the opening/closing statuses to trigger a siren to turn on before the door starts moving."
			
			input "operatingDelay", "enum",
				title: "Select Operating Delay:",
				required: false,
				defaultValue: operatingDelaySetting,
				options: operatingDelayOptions
			
			paragraph ""
		}
		
		section("<b>Garage Door Operating Duration</b>") {
			paragraph "The Operating Duration determines the amount of time the door control child device should stay in the OPENING and CLOSING states."
			
			input "operatingDuration", "enum",
				title: "Select Operating Duration:",
				required: false,
				defaultValue: operatingDurationSetting,
				options: operatingDurationOptions
			
			paragraph ""			
		}	
		
		section("<b>Logging</b>") {			
			input "debugLogging", "bool", 
				title: "Enable debug logging?",
				defaultValue: true, 
				required: false
		}	
	}
}


def pageRemove() {
	dynamicPage(name: "pageRemove", title: "", install: false, uninstall: true) {
		section() {			
			paragraph "WARNING: You are about to remove the Zooz Garage Door Controller SmartApp and ALL of the Garage Door Opener devices it created.", required: true, state: null
		}
	}
}


def installed() {
	log.warn "installed()..."

	state.installed = true

	initialize()
}

def updated() {		
	log.warn "updated()..."

	unsubscribe()
	unschedule()
	initialize()
}

void initialize() {
	if (!childDoorOpener) {			
		runIn(3, createChildGarageDoorOpener)
	}			
	subscribe(settings?.relaySwitch, "switch.on", relaySwitchOnEventHandler)
	subscribe(settings?.contactSensor, "contact", contactEventHandler)		
}

void createChildGarageDoorOpener() {
	def name = "${app.label} Opener"
	logDebug "Creating ${name}"	

	try {
		def child = addChildDevice(
			"krlaframboise",
			"Child Garage Door Opener",
			"${app.id}-door",
			null,
			[
				name: "${name}",
				label: "${name}",
				completedSetup: true
			]
		)

		checkDoorStatus()
	}
	catch (ex) {
		log.error "Unable to create Garage Door Opener.  You must install and publish the Child Garage Door Opener Device Handler in order to use this SmartApp."
	}
}


def childUninstalled() {
	logTrace "childUninstalled()..."
}


void childRefresh(childDNI) {
	logTrace "childRefresh()..."	
	checkDoorStatus()
}


void childOpen(childDNI) {	
	handleDigitalOpenCloseCommand("opening")
}

void childClose(childDNI) {	
	handleDigitalOpenCloseCommand("closing")
}

void handleDigitalOpenCloseCommand(doorStatus) {
	logTrace "handleDigitalOpenCloseCommand(${doorStatus})"	
	
	String newContactStatus = (doorStatus == "opening" ? "open" : "closed")
	String oldDoorStatus = childDoorOpener?.currentValue("door")
	String oldContactStatus = settings?.contactSensor?.currentValue("contact")
	
	if ((newContactStatus != oldContactStatus) || (newContactStatus != oldDoorStatus)) {
		sendDoorEvents(doorStatus)		
		runIn(operatingDelaySetting, turnOnRelaySwitch)	
	}
	else {
		logDebug "The Door is already ${newContactStatus}"
	}
	
	runIn((operatingDelaySetting + operatingDurationSetting), checkDoorStatus)
}


void turnOnRelaySwitch() {
	logDebug "Turning on Relay Switch..."
	settings?.relaySwitch?.on()
}


void relaySwitchOnEventHandler(evt) {
	logDebug "Relay Switch Turned ${evt.value}"
	
	if (autoOffDelaySetting) {
		runIn(autoOffDelaySetting, turnOffRelaySwitch)
	}
	
	switch (childDoorOpener?.currentValue("door")) {
		case "open":
			sendDoorEvents("closing")
			break
		case "closed":
			sendDoorEvents("opening")		
			break
	}
	
	runIn(operatingDurationSetting, checkDoorStatus)
}


void turnOffRelaySwitch() {
	logDebug "Turning off Relay Switch..."
	settings?.relaySwitch?.off()
}


void contactEventHandler(evt) {
	logDebug "Contact sensor changed to ${evt.value}"
	String doorStatus = childDoorOpener?.currentValue("door")
	
	if ((evt.value == "open") && (doorStatus == "opening")) {
		// open usually happens immediately so let the scheduled door status check change it.
	}
	else if ((evt.value == "closed") && (doorStatus == "closing")) {
		sendDoorEvents("closed")
	}
	else {
		if ((evt.value == "open") && (doorStatus == "closed")) {
			// Door manually opened or relay failed to report ON when physical switch pushed
			sendDoorEvents("opening")
		}
		else if ((evt.value == "closed") && (doorStatus == "open")) {
			// Door manually closed or relay failed to report ON when physical switch pushed
			sendDoorEvents("closing")
		}
		runIn(operatingDurationSetting, checkDoorStatus)
	}
	
	// backup check in case there was a timing issue resulting in the status getting stuck as opening or closing.
	runIn(operatingDurationSetting + 3, checkDoorStatus, [overwrite: false])
}


void checkDoorStatus() {
	logTrace "checkDoorStatus()..."
	
	String contactStatus = settings?.contactSensor?.currentValue("contact")
	
	if (childDoorOpener?.currentValue("door") != contactStatus) {		
		sendDoorEvents(contactStatus)		
	}	
}


void sendDoorEvents(value) {
	def doorOpener = childDoorOpener		
	doorOpener?.parse([name: "door", value: value, displayed: true])
	
	if (value in ["open", "closed"]) {
		doorOpener?.parse([name: "contact", value: value, displayed: false])
	}
}


def getChildDoorOpener() {
	def children = childDevices
	return children ? children[0] : null
}


Integer getAutoOffDelaySetting() {
	return safeToInt((settings ? settings["autoOffDelay"] : null), 2)
}

Integer getOperatingDurationSetting() {
	return safeToInt((settings ? settings["operatingDuration"] : null), 15)
}

Integer getOperatingDelaySetting() {
	return safeToInt((settings ? settings["operatingDelay"] : null), 0)
}


Integer safeToInt(val, Integer defaultVal=0) {
	if ("${val}"?.isInteger()) {
		return "${val}".toInteger()
	}
	else if ("${val}".isDouble()) {
		return "${val}".toDouble()?.round()
	}
	else {
		return  defaultVal
	}
}

Long safeToLng(val, Long defaultVal=0) {
	if ("${val}"?.isLong()) {
		return "${val}".toLong()
	}
	else if ("${val}".isBigDecimal()) {
		return "${val}".toBigDecimal()?.round()
	}
	else {
		return  defaultVal
	}
}


void logDebug(String msg) {
	if (settings?.debugLogging != false) {
		log.debug "$msg"
	}
}

void logTrace(String msg) {
	// log.trace "$msg"
}
