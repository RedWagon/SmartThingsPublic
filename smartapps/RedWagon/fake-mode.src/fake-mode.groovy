definition(
    name: "Fake Mode",
    namespace: "fake-modes/child",
    author: "RedWagon",
    description: " ",
    category: "Convenience",
    iconUrl: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg",
    iconX2Url: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg"
)

preferences {
    section(title: "Inputs") {
        input "switches", "capability.switches", title: "Switches", multiple: true, required: false, hideWhenEmpty: true
    }
    section(title: "Device States") {
        input "temp", "number", title: "White Temperature", required:false, description: "idk the range yet"
        input "temp_level", "enum", title: "White Level", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        input "color", "enum", title: "Color", required: false, multiple:false, options: [
                ["Soft White":"Soft White - Default"],
                ["White":"White - Concentrate"],
                ["Daylight":"Daylight - Energize"],
                ["Warm White":"Warm White - Relax"],
                ["Red": "Red"],
                ["Green": "Green"],
                ["Blue": "Blue"],
                ["Yellow": "Yellow"],
                ["Orange": "Orange"],
                ["Purple": "Purple"],
                ["Pink": "Pink"]
            ]
        input "color_level", "enum", title: "Color Level", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        input "dimmer_level", "enum", title: "Dimmer Level", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        input "switch_state", "enum", title: "Switch State", required: false, options: [[true:"On"],[false:"Off"]]
    }
}


/** Constants for Hue Colors
Map getHueColors() {
    return [Red: 0, Green: 39, Blue: 70, Yellow: 25, Orange: 10, Purple: 75, Pink: 83]
}
*/

def active() {
    def currSwitches = switches.currentSwitch

    def onSwitches = currSwitches.findAll { switchVal ->
        switchVal == "on" ? true : false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}."
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}."
	//unsubscribe()
	//unschedule()
	//initialize()
}

def initialize() {
	log.debug "Initialized with settings: ${settings}."
}

def getHueColor() {

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
    log.debug "new value = $newValue"
    return newValue = [hue: hueColor, saturation: saturation, level: color_level as Integer ?: 100]
}
