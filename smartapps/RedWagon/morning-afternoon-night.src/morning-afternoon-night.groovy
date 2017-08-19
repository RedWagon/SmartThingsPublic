definition(
    name: "Bright When Dark And/Or Bright After Sunset",
    namespace: "Arno",
    author: "Arnaud",
    description: "Turn ON light(s) and/or dimmer(s) when there's movement and the room is dark with illuminance threshold and/or between sunset and sunrise. Then turn OFF after X minute(s) when the brightness of the room is above the illuminance threshold or turn OFF after X minute(s) when there is no movement.",
    category: "Convenience",
    iconUrl: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg",
    iconX2Url: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg"
)

preferences {
	page(name: "configurations")
	page(name: "options")

	page(name: "timeIntervalInput", title: "Only during a certain time...") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
        }
    }
}

def configurations() {
	dynamicPage(name: "configurations", title: "Configurations...", uninstall: true, nextPage: "options") {
		section(title: "Turn ON lights on movement when...") {
			input "dark", "bool", title: "It is dark?", required: true
            input "sun", "bool", title: "Between sunset and surise?", required: true
        }
		section(title: "More options...", hidden: hideOptionsSection(), hideable: true) {
			def timeLabel = timeIntervalLabel()
			href "timeIntervalInput", title: "Only during a certain time:", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
			input "days", "enum", title: "Only on certain days of the week:", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is:", multiple: true, required: false 
        }
        section("Control these light(s)...") {
            input "light_lights", "capability.switch", title: "Light(s) when light?", multiple: true, required: false
            input "dark_lights", "capability.switch", title: "Light(s) when dark?", multiple: true, required: false
        }    
        section("Control these dimmer(s)...") { 
            input "dimmers", "capability.switchLevel", title: "Dimmer(s)?", multiple: true, required:false
            input "light_dimLevel", "number", title: "How bright when light?", required:false, description: "0% to 100%"
            input "dark_dimLevel", "number", title: "How bright when dark?", required:false, description: "0% to 100%"
        }
        section("Control these hue bulbs...") {
            input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required: false, multiple: true
            input "light_color", "enum", title: "Hue Color?", required: false, multiple:false, options: [
                ["Soft White":"Soft White - Default"],
                ["White":"White - Concentrate"],
                ["Daylight":"Daylight - Energize"],
                ["Warm White":"Warm White - Relax"],
                "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "light_colorLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
            input "dark_color", "enum", title: "Hue Color?", required: false, multiple:false, options: [
                ["Soft White":"Soft White - Default"],
                ["White":"White - Concentrate"],
                ["Daylight":"Daylight - Energize"],
                ["Warm White":"Warm White - Relax"],
                "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "dark_colorLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        }
		section ("Assign a name") {
			label title: "Assign a name", required: false
        }
    }
}

/** Constants for Hue Colors */
Map getHueColors() {
    return [Red: 0, Green: 39, Blue: 70, Yellow: 25, Orange: 10, Purple: 75, Pink: 83]
}

def options() {
    dynamicPage(name: "options", title: "Lights will turn ON on movement...", install: true, uninstall: true) {
        section("Turning ON when there's movement...") {
            input "motionSensor", "capability.motionSensor", title: "Where?", multiple: true, required: true
        } 
        section("And then OFF when there's been no movement for...") {
            input "delayMinutes", "number", title: "Minutes?", required: false
        }
    }
    if (dark == true) {
    	dynamicPage(name: "options", title: "Lights will turn ON on movement when it is dark...", install: true, uninstall: true) {
			section("Using this light sensor...") {
				input "lightSensor", "capability.illuminanceMeasurement",title: "Light Sensor?", multiple: false, required: true
        		input "luxLevel", "number", title: "Illuminance threshold? (default 50 lux)",defaultValue: "50", required: false
            }
        }
    }
    if (sun == true) {
    	dynamicPage(name: "options", title: "Lights will turn ON on movement between sunset and sunrise...", install: true, uninstall: true) {
			section ("Between sunset and sunrise...") {
				input "sunriseOffsetValue", "text", title: "Sunrise offset", required: false, description: "00:00"
				input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
        		input "sunsetOffsetValue", "text", title: "Sunset offset", required: false, description: "00:00"
				input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
            }
			section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
				input "zipCode", "text", title: "Zip Code?", required: false, description: "Local Zip Code"
            }
        }
    }

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
	subscribe(motionSensor, "motion", motionHandler)
    if (light_lights != null && light_lights != "") {
        log.debug "$light_lights subscribing..."
    	subscribe(light_lights, "switch", lightLightsHandler)
    }
    if (dark_lights != null && dark_lights != "") {
        log.debug "$dark_lights subscribing..."
    	subscribe(dark_lights, "switch", darkLightsHandler)
    }
    if (dimmers != null && dimmers != "") {
        log.debug "$dimmers subscribing..."
    	subscribe(dimmers, "switch", dimmersHandler)
    }
    if (hues != null && hues != "") {
        log.debug "$hues subscribing..."
    	subscribe(hues, "switch", huesHandler)
    }
    if (dark == true && lightSensor != null && lightSensor != "") {
        log.debug "$light_lights and $dark_lights and $dimmers and $hues will turn ON when movement detected and when it is dark..."
        subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
    }
    if (sun == true) {
        log.debug "$light_lights and $dark_lights and $dimmers and $hues will turn ON when movement detected between sunset and sunrise..."
        astroCheck()
        subscribe(location, "position", locationPositionChange)
        subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
        subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
    }

    log.debug "Determinating lights and dimmers current value..."
    if (light_lights != null && light_lights != "") {
        state.light_lightsState = "off"
        if (light_lights.currentValue("switch").toString().contains("on")) {
            state.light_lightsState = "on"
        }
        log.debug "Lights $state.light_lightsState."
    }
    if (dark_lights != null && dark_lights != "") {
        state.dark_lightsState = "off"
        if (dark_lights.currentValue("switch").toString().contains("on")) {
            state.dark_lightsState = "on"
        }
        log.debug "Lights $state.dark_lightsState."
    }
    if (dimmers != null && dimmers != "") {
        state.dimmersState = "off"
        if (dimmers.currentValue("switch").toString().contains("on")) {
            state.dimmersState = "on"
        } 
        log.debug "Dimmers $state.dimmersState."
    }
    if (hues != null && hues != "") {
        state.huesState = "off"
        if (hues.currentValue("switch").toString().contains("on")) {
            state.huesState = "on"
        } 
        log.debug "Hues $state.huesState."
    }
}
            
def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	state.lastSunriseSunsetEvent = now()
	log.debug "SmartNightlight.sunriseSunsetTimeHandler($app.id)"
	astroCheck()
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
        unscheduleAll()
        if (dark == true) {
            if (darkOk == true) {
                turnOn()
                return
            } 
        }
        if (sun == true) {
            if (sunOk == true) {
                turnOn()
                return
            }
        }
        if (dark != true && sun != true) {
            log.debug "Lights and dimmers will turn ON because $motionSensor detected motion..."
            turnOn()
        }
    } 
    if (evt.value == "inactive") {
        unscheduleAll()
        if (delayMinutes) {
            def delay = delayMinutes * 60
            log.debug "Lights and dimmers will turn OFF in $delayMinutes minute(s)..."
            turnOff(delay)
        }
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
    if (state.huesState == "on") {
        log.debug "Hues: $hues already ON."
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
    if (state.huesState == "off") {
        log.debug "Hues: $hues already OFF."
        return
    }
    log.debug "Turning OFF hues: $hues..."
    //dimmers?.off()
    state.huesState = "off"
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
