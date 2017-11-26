definition(
    name: "Fake Modes Controller",
    namespace: "fake-modes/parent",
    author: "RedWagon",
    description: " ",
    category: "Convenience",
    iconUrl: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg",
    iconX2Url: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg"
)

preferences {
    device_inputs()
    sensor_inputs()
    detail_inputs()
    app(name: "fakeMode", appName: "Fake Mode", namespace: "fake-modes/child", title: "Create New Mode", multiple: true)
}

def detail_inputs() {
    input "timeout", "number", title: "Normal timeout?", required: false
    input "hard_timeout", "number", title: "Hard timeout?", required: false
}

def device_inputs() {
    section(title: "Devices") {
        input "temps", "capability.colorTemperature", title: "Hue bulbs (temp control)", required: false, multiple: true, hideWhenEmpty: true
        input "colors", "capability.colorControl", title: "Hue bulbs (color control)", required: false, multiple: true, hideWhenEmpty: true
        input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required:false, hideWhenEmpty: true
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false, hideWhenEmpty: true
    }
    section(title: "Optional Devices") {
        input "optionals", "capability.switch", title: "Optional Devices", multiple: true, required: false, hideWhenEmpty: true
    }
}

def sensor_inputs() {
    section(title: "Positive Events") {
        input "sensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false, hideWhenEmpty: true
        input "open_doors", "capability.contactSensor", title: "Open Doors", multiple: true, required: false, hideWhenEmpty: true
        input "closed_doors", "capability.contactSensor", title: "Closed Doors", multiple: true, required: false, hideWhenEmpty: true
        input "push_buttons", "capability.button", title: "Push Buttons", multiple: true, required: false, hideWhenEmpty: true
        input "hold_buttons", "capability.button", title: "Hold Buttons", multiple: true, required: false, hideWhenEmpty: true
    }
    section(title: "Manual Off Events") {
        input "open_doors_off", "capability.contactSensor", title: "Open Doors", multiple: true, required: false, hideWhenEmpty: true
        input "closed_doors_off", "capability.contactSensor", title: "Closed Doors", multiple: true, required: false, hideWhenEmpty: true
        input "push_buttons_off", "capability.button", title: "Push Buttons", multiple: true, required: false, hideWhenEmpty: true
        input "hold_buttons_off", "capability.button", title: "Hold Buttons", multiple: true, required: false, hideWhenEmpty: true
    }
}

/** Constants for Hue Colors */
Map getHueColors() {
    return [Red: 0, Green: 39, Blue: 70, Yellow: 25, Orange: 10, Purple: 75, Pink: 83]
}

def installed() {
	log.debug "Installed with settings: ${settings}."
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}."
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	subscribe(temps, "colorTemperature", rogueHandler)
	subscribe(colors, "colorControl", rogueHandler)

	subscribe(temps, "switchLevel", rogueHandler)
	subscribe(colors, "switchLevel", rogueHandler)
	subscribe(dimmers, "switchLevel", rogueHandler)

	subscribe(temps, "switch", rogueHandler)
	subscribe(colors, "switch", rogueHandler)
	subscribe(dimmers, "switch", rogueHandler)
	subscribe(switches, "switch", rogueHandler)

    subscribe(sensors, "motion.active", onHandler)
    subscribe(open_doors, "contact.open", onHandler)
    subscribe(closed_doors, "contact.closed", onHandler)
    subscribe(push_buttons, "button.pushed", onHandler)
    subscribe(held_buttons, "button.held", onHandler)

    subscribe(sensors, "motion.inactive", timeoutHandler)

    subscribe(open_doors_off, "contact.open", offHandler)
    subscribe(closed_doors_off, "contact.closed", offHandler)
    subscribe(push_buttons_off, "button.pushed", offHandler)
    subscribe(held_buttons_off, "button.held", offHandler)
}

def rogueHandler(evt) {
	log.debug "Rogue $evt.displayName $evt.name: $evt.value"
}
def onHandler(evt) {
	log.debug "On $evt.displayName $evt.name: $evt.value"
}
def timeoutHandler(evt) {
	log.debug "Timeout $evt.displayName $evt.name: $evt.value"
}
def offHandler(evt) {
	log.debug "Off $evt.displayName $evt.name: $evt.value"
}

def writeMode(mode) {
}

def topMode() {
    modes = getChildApps()
    modes.each { mode ->
        if (mode.active()) {
            return mode
        }
    }
}
            
//old stuff

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
        unscheduleAll()
        if (dark == true) {
            if (darkOk == true) {
                turnOnDark()
                return
            } else {
                turnOnLight()
                return
            }
        }
        if (sun == true) {
            if (sunOk == true) {
                turnOnDark()
                return
            } else {
                turnOnLight()
                return
            }
        }
        if (dark != true && sun != true) {
            log.debug "Lights and dimmers will turn ON because $motionSensor detected motion..."
            turnOnLight()
        }
    } 
    if (evt.value == "inactive") {
        unscheduleAll()
        if (dark != true && sun != true) {
            turnOnDark()
            return
        }
        def delay = delayMinutes * 60
        log.debug "Lights and dimmers will turn OFF in $delayMinutes minute(s)..."
        turnOff(delay)
    }
}

def lightLightsHandler(evt) {
	log.debug "Light Lights Handler $evt.name: $evt.value"
    if (evt.value == "on") {
        log.debug "Light Lights: $light_lights now ON."
        unschedule(turnOffLightLights)
        state.light_lightsState = "on"
    } else if (evt.value == "off") {
        log.debug "Light Lights: $light_lights now OFF."
        unschedule(turnOffLightLights)
        state.light_lightsState = "off"
    }
}

def darkLightsHandler(evt) {
	log.debug "Dark Lights Handler $evt.name: $evt.value"
    if (evt.value == "on") {
        log.debug "Dark Lights: $dark_lights now ON."
        unschedule(turnOffDarkLights)
        state.dark_lightsState = "on"
    } else if (evt.value == "off") {
        log.debug "Dark Lights: $dark_lights now OFF."
        unschedule(turnOffDarkLights)
        state.dark_lightsState = "off"
    }
}

def dimmersHandler(evt) {
	log.debug "Dimmer Handler $evt.name: $evt.value"
    if (evt.value == "on") {
        log.debug "Dimmers: $dimmers now ON."
        unschedule(turnOffDimmers)
        state.dimmersState = "on"
    } else if (evt.value == "off") {
        log.debug "Dimmers: $dimmers now OFF."
        unschedule(turnOffDimmers)
        state.dimmersState = "off"
    }
}

def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus lights: $state.light_lightsState, dark lights: $state.dark_lightsState, lastStatus dimmers: $state.dimmersState, motionStopTime: $state.motionStopTime"
	unscheduleAll()
    if (evt.integerValue > 999) {
        log.debug "Lights and dimmers will turn OFF because illuminance is superior to 999 lux..."
        turnOffLights()
        turnOffDimmers()
        turnOffHues()
    } else if (evt.integerValue > ((luxLevel != null && luxLevel != "") ? luxLevel : 50)) {
		log.debug "Lights and dimmers will turn OFF because illuminance is superior to $luxLevel lux..."
        turnOffLights()
        turnOffDimmers()
        turnOffHues()
    }
}


def turnOnLight() {
	if (!allOk) {
        log.debug "Time, days of the week or mode out of range! lights will not turn ON."
        return
    }
    turnOnLightLights()
    turnOnDimmers(light_dimLevel)
    turnOnHues(light_color, light_colorLevel)
}

def turnOnDark() {
	if (!allOk) {
        log.debug "Time, days of the week or mode out of range! lights will not turn ON."
        return
    }
    turnOnDarkLights()
    turnOnDimmers(dark_dimLevel)
    turnOnHues(dark_color, dark_colorLevel)
}

def turnOff(delay) {
	if (!allOk) {
        log.debug "Time, days of the week or mode out of range! lights will not turn OFF."
        return
    }
    runIn(delay, turnOffLightLights)
    runIn(delay, turnOffDarkLights)
    runIn(delay, turnOffDimmers)
    runIn(delay, turnOffHues)
}

def unscheduleAll() {
    unschedule(turnOffLightLights)
    unschedule(turnOffDarkLights)
    unschedule(turnOffDimmers)
    unschedule(turnOffHues)
}

def turnOnDarkLights() {
    if (dark_lights == null || dark_lights == "") {
        log.debug "DarkLights: none to turn on..."
        return
    }
    if (state.dark_lightsState == "on") {
        log.debug "DarkLights: $dark_lights already ON."
        return
    }
    log.debug "Turning ON dark_lights: $dark_lights..."
    dark_lights?.on()
    state.dark_lightsState = "on"
}

def turnOnLightLights() {
    if (light_lights == null || light_lights == "") {
        log.debug "LightLights: none to turn on..."
        return
    }
    if (state.light_lightsState == "on") {
        log.debug "LightLights: $light_lights already ON."
        return
    }
    log.debug "Turning ON light_lights: $light_lights..."
    light_lights?.on()
    state.light_lightsState = "on"
}

def turnOnDimmers(dimLevel) {
    if (dimmers == null || dimmers == "") {
        log.debug "Dimmers: none to turn on..."
        return
    }
    if (state.dimmersState == "on") {
        log.debug "Dimmers: $dimmers already ON."
        return
    }
    log.debug "Turning ON dimmers: $dimmers..."
    settings.dimmers?.setLevel(dimLevel)
    state.dimmersState = "on"
}

def turnOnHues(color, colorLevel) {
    if (hues == null || hues == "") {
        log.debug "Hues: none to turn on..."
        return
    }
    log.debug "Turning ON hues: $hues..."
    state.huesState = "on"

    def hueColor = 0
    def saturation = 100

    switch(color) {
        case "White":
            hueColor = 52
            saturation = 19
            break;
        case "Daylight":
            hueColor = 53
            saturation = 91
            break;
        case "Soft White":
            hueColor = 23
            saturation = 56
            break;
        case "Warm White":
            hueColor = 20
            saturation = 80 //83
            break;
        case "Blue":
            hueColor = 70
            break;
        case "Green":
            hueColor = 39
            break;
        case "Yellow":
            hueColor = 25
            break;
        case "Orange":
            hueColor = 10
            break;
        case "Purple":
            hueColor = 75
            break;
        case "Pink":
            hueColor = 83
            break;
        case "Red":
            hueColor = 100
            break;
    }
    def newValue = [hue: hueColor, saturation: saturation, level: colorLevel as Integer ?: 100]
    log.debug "new value = $newValue"

    hues*.setColor(newValue)
}

def turnOffLights() {
    turnOffDarkLights()
    turnOffLightLights()
}

def turnOffDarkLights() {
    if (dark_lights == null || dark_lights == "") {
        log.debug "DarkLights: none to turn off..."
        return
    }
    if (state.dark_lightsState == "off") {
        log.debug "DarkLights: $dark_lights already OFF."
        return
    }
    log.debug "Turning OFF dark_lights: $dark_lights..."
    dark_lights?.off()
    state.dark_lightsState = "on"
}

def turnOffLightLights() {
    if (light_lights == null || light_lights == "") {
        log.debug "LightLights: none to turn off..."
        return
    }
    if (state.light_lightsState == "off") {
        log.debug "LightLights: $light_lights already OFF."
        return
    }
    log.debug "Turning OFF light_lights: $light_lights..."
    light_lights?.off()
    state.light_lightsState = "on"
}

def turnOffDimmers() {
    if (dimmers == null || dimmers == "") {
        log.debug "Dimmers: none to turn off..."
        return
    }
    if (state.dimmersState == "off") {
        log.debug "Dimmers: $dimmers already OFF."
        return
    }
    log.debug "Turning OFF dimmers: $dimmers..."
    dimmers?.off()
    state.dimmersState = "off"
}

def turnOffHues() {
    if (hues == null || hues == "") {
        log.debug "Hues: none to turn off..."
        return
    }
    log.debug "Turning OFF hues: $hues..."
    hues?.off()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "Sunrise: ${new Date(state.riseTime)}($state.riseTime), Sunset: ${new Date(state.setTime)}($state.setTime)"
}

private getDarkOk() {
	def result
	if (dark == true && lightSensor != null && lightSensor != "") {
		result = lightSensor.currentIlluminance < ((luxLevel != null && luxLevel != "") ? luxLevel : 50)
    }
	log.trace "darkOk = $result"
	result
}

private getSunOk() {
	def result
	if (sun == true) {
		def t = now()
		result = t < state.riseTime || t > state.setTime
    }
	log.trace "sunOk = $result"
	result
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
        } else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
		def day = df.format(new Date())
		result = days.contains(day)
    }
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    }
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
