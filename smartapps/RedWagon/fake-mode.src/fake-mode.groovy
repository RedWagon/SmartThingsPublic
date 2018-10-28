definition(
    name: "Fake Mode",
    namespace: "fake-modes/child",
    author: "RedWagon",
    description: "Child app to be used with Fake Modes.  Define a set of color/states",
    category: "Convenience",
    iconUrl: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg",
    iconX2Url: "http://neiloseman.com/wp-content/uploads/2013/08/stockvault-bulb128619.jpg"
)

preferences {
    section(title: "Inputs") {
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false, hideWhenEmpty: true
        input "order", "enum", title: "Priority", required: false, options: [[1:"First"],[2:"2"],[3:"3"],[4:"4"],[5:"5"],[6:"6"],[7:"7"],[8:"8"],[9:"Last"]]
    }
    section(title: "Device States") {
        input "bow_step", "number", title: "Rainbow Hue Step", required:false, description: "Add this to hue value"
        input "bow_delay", "number", title: "Rainbow Delay per Step", required:false, description: "Wait this many seconds before adding"
        input "temp", "number", title: "White Temperature", required:false, description: "idk the range yet"
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
        input "dimmer_level", "enum", title: "Dimmer Level", required: false, options: [[0: "0%"], [10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        input "switch_state", "enum", title: "Switch State", required: false, options: [[true:"On"],[false:"Off"]]
    }
}


/** Constants for Hue Colors
Map getHueColors() {
    return [Red: 0, Green: 39, Blue: 70, Yellow: 25, Orange: 10, Purple: 75, Pink: 83]
}
*/

def active() {
    if (!switches) {
        log.debug "No active switches, defaulting to on"
        return true
    }
    log.debug "Checking active state of switches"
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
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	log.debug "Initialized with settings: ${settings}."
    subscribe(switches, "switch", triggerParent)
}

def triggerParent(evt) {
    log.debug "Mode triggered"
    parent.auto_on()
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
    def newValue = [hue: hueColor, saturation: saturation]
    log.debug "new value = $newValue"
    return newValue
}
