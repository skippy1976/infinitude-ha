/*
 * Infinitude App
 *
 * Initial release: 
 */

import java.time.LocalTime
import java.time.format.DateTimeFormatter

def version() { return "Infinitude Hubitat App 0.0.1" }

definition(
    name: "Infinitude Integration",
    namespace: "Infinitude",
    author: "skippy76",
    description: "Creates virtual devices for Carrier / Bryant thermostats.",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/skippy1976/infinitude-ha/refs/heads/main/infinitude-app.groovy",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

preferences {
	page(name: "mainPage")
    page(name: "systemsPage", nextPage: "mainPage")
    page(name: "editSytemPage", nextPage: "systemsPage")
    page(name: "aboutPage", nextPage: "mainPage")
}

//App Pages/Views
def mainPage() {
	ifDebug("Main page");
    state.isDebug = isDebug
    def systemCount = getChildDevices().size();
    ifDebug("System device count: ${systemCount}")
    
	if (!state.infinitudeIntegrationInstalled || systemCount == 0) {
		ifDebug("Initial main page")

		return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {        
			showTitle()
 			section("Define your Infinitude system") {
                clearStateVariables()
                input name: "InfinitudeIP", type: "text", title: "Infinitude Server IP Address", required: true, submitOnChange: true
                input name: "InfinitudePort", type: "text", title: "Infinitude Server Port Number",  defaultValue: "3000", required: true, submitOnChange: true
                input name: "isDebug", type: "bool", title: "Enable debug logging", defaultValue: true, submitOnChange: true
            }
        }
    } else {
		ifDebug("Initialized main page")
        
		return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
			showTitle()
            
			section("<h1>Systems</h1>") {
                href (name: "systemsPage",
                      title: "Systems",
                      description: "Add names for your systems",
                      page: "systemsPage")
            }
        
			section("<h1>About</h1>") {
				href (name: "aboutPage",
                      title: "About",
					  description: "Find out more about Infinitude Integration",
					  page: "aboutPage")
			}
        
			section("") {
                input name: "logEnable", type: "bool", title: "Enable debug logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
			}            
        }
    }
}

// about page
def aboutPage() {
    ifDebug("Showing aboutPage")

	dynamicPage(name: "aboutPage", title: none){
        section("<h1>Introducing Infinitude Integration</h1>"){
            paragraph "A module that provide access to Infinitude..." +
                " in turn providing access to your Carrier Infinity or Bryant Evolution HVAC system."
            paragraph "You must already have Infinitude setup and operational with your Infinity / Evolution ssytem."
        }
	}
}

// page for editing systems names
def systemsPage() {
    ifDebug("Showing systemsPage")
    
    def systemDevice = getChildDevice()
	ifDebug("getChildDevice: ${childDevices}")
    
    if (getChildDevices().size() == 0 && !state.infinitudeIntegrationInstalled)
    {
        log.warn ("not initialised")
        return
    }
    
    if (state.editingSystem)
    {
        editSystem()
    }

	dynamicPage(name: "systemsPage", title: "", install: false, uninstall: false){

        section("<h1>Systems</h1>"){
            paragraph "Potentially your home may consist of multiple seperate Infinity or Evolution systems" + 
				"Note that these are different and should not be confused with zones. Each system may consist of multiple zones."
            paragraph "You will want to provide a name for each system to ensure that it is something meaningful. " +
                "The system name is the parent device in the Hubitat devices that will have all the zone thermostat devices."

            getChildDevices().each{
				href (name: "editSytemPage", title: "${it.label} (${it.name})",
					  description: "System Details\t${it.getTypeName()}",
					  params: [deviceNetworkId: it.deviceNetworkId],
					  page: "editSytemPage")
            }
		}
	}
}

// edit system page
def editSytemPage(message) {
    ifDebug("Showing editSystemPage")
    ifDebug("editing ${message.deviceNetworkId}")
    
    state.editingSystem = true
    state.editedDeviceNetworkId = message.deviceNetworkId;
    def systemDevice = getChildDevice(message.deviceNetworkId)
    
	ifDebug("editing systemDevice: name = '${systemDevice.name}' label = '${systemDevice.label}' ")
    
    dynamicPage(name: "editSytemPage", title: ""){
        section("<h1>Edit an Infinitude System</h1>"){
			paragraph "${systemDevice.label}"
			paragraph paragraphText
            paragraph ""
            input "newSystemLabel", "text", title: "System Name", required: true, multiple: false, defaultValue: systemDevice.name, submitOnChange: false
		}
    }
}

def editSystem(){
    ifDebug("editSystem updating")
	def childSystem = getChildDevice(state.editedDeviceNetworkId);

    if (childSystem) {
        ifDebug("renaming system from ${childSystem.label()} to ${newSystemLabel}")
		childSystem.setLabel(newSystemLabel)
    } else {
        log.warn("system device not found for update ${state.editedDeviceNetworkId}")
    }

    state.editingSystem = false
	state.editedDeviceNetworkId = null;
}

// show title for pages
def showTitle(){
	def appVersion = version()
	def driverVersion = (getInfinitudeSystemDevice(0) == null) ? "Not Yet Installed" : getInfinitudeSystemDevice(0).version()
	state.version = " App [" + appVersion + "]  Driver [" + driverVersion + "]"
	section(){paragraph "<br> Version: $state.version <br>"}
}

// clear settings variables
def clearStateVariables(){
	ifDebug("Clearing Settings Variables just in case.")
    settings.InfinitudeIP = null
    settings.InfinitudePort = null
    settings.logEnable = null
}

def getInfinitudeSystemDevice(idx){
    ifDebug("getInfinitudeSystemDevice ${idx}")
    def infinitudeSystem = getChildDevice("infinitude-${idx}")
	return infinitudeSystem
}

// called when installing
void installed()
{
    ifDebug("Installed")
    state.infinitudeIntegrationInstalled = true

    unsubscribe()
    updated()
}

// Called when uninstalling
void uninstalled()
{
    clearStateVariables()
    // unsubscribe to all events
    ifDebug("Uninstalled")
    removeChildDevices(getChildDevices())
    unsubscribe()
}

void eventHandler(evt)
{
    ifDebug("event received")
}

// remove all child devices
private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

// perform http get from Infinitude
def httpGetExec(webserviceurl)
{
    ifDebug("httpGetExec()") 
    try {
        def params = [
           uri: webserviceurl,
           contentType: "application/json"
        ]
        
        httpGet(params) { resp ->
            if (resp.data) {
                ifDebug("resp.data = ${resp.data}")
                return resp.data
            }
        }
    }
    catch (Exception e) {
        log.warn("httpGetExec() failed: ${e.message}")
    }
}

// Create a parent infinitude system device
def createInfinitudeSystem(systemId){    
    // check if it already exists
    if (getInfinitudeSystemDevice(systemId) == null){
        ifDebug("Creating System Device ${systemId} infinitude-${systemId}")
        addChildDevice("Infinitude", "Infinitude System", "infinitude-${systemId}", null, [name: "Infinitude Ssytem ${systemId}", isComponent: true, label: "Infinitude Ssytem ${systemId}"])
    } else {
        ifDebug("Infinitude system with ID ${systemId} already exists")
    }
    
    return getInfinitudeSystemDevice(systemId)
}

def getSystems() {
    String s = "http://" + settings.InfinitudeIP + ":" + settings.InfinitudePort + "/systems.json"
    ifDebug("Infinitude request ${s}")
    def systems = httpGetExec(s)   
}

def updateSystems(systems) {
    String s = "http://" + settings.InfinitudeIP + ":" + settings.InfinitudePort
    ifDebug("Infinitude request ${s}")
    httpPost(uri: s,
                 path: "/systems/infinitude",
                 contentType: 'application/json;charset=UTF-8',
                 requestContentType: 'application/json;charset=UTF-8',
                 body: systems
                ) { response ->
             
             ifDebug("hub remote id: ${response.status}")
             return
    	}
}
	
def getSystemStatus() {
    s = "http://" + settings.InfinitudeIP + ":" + settings.InfinitudePort + "/status.json"
    ifDebug("Infinitude request ${s}")
    def systemStatus = httpGetExec(s)   
}

// called following update
void updated()
{    
    ifDebug("Updated [${settings.InfinitudeIP}]")
    ifDebug("Updated [${settings.InfinitudePort}]")

    settings.InfinitudrIP = settings.InfinitudeIP.trim()
    settings.InfinitudrPort = settings.InfinitudePort.trim()

    def systems = getSystems()
    def systemStatus = getSystemStatus()
    
    int systemsSize = systems.size()

    if (systemsSize == 0) {
		log.warn "systems is empty list"
		return
    } else {
    	ifDebug("Infinitude systems ${systemsSize}")
    }
    
    // user may have multiple systems
    systems.system.eachWithIndex { system, idx ->
        ifDebug("Infinitude system version: ${system.version}")

        // Create each system
        def infinitudeSystem = createInfinitudeSystem(idx)
        def systemConfig = system.config[0]
        
        // a config includes the system config and the zone config, the zones are child devices of the system
        // get existing zones of the system device
        def zones = infinitudeSystem.getChildDevices();
        def zonesCount = 0
        
        if (zones != null) {
        	ifDebug("Existing zone devices for system infinitude-${idx} count: ${zones.size()}")
        
       		if (zones.size() > 0) {
				zones.eachWithIndex { zone, zoneIdx ->
                    ifDebug("Updating existing zone ${zone}")
                    infinitudeSystem.updateZone(systemConfig.zones[0].zone[zoneIdx], "infinitude-${idx}-${zoneIdx}", systemStatus.status[idx].zones[0].zone[zoneIdx], systemConfig)
				}
                zonesCount = zones.size()
        	} else {
            	ifDebug("No existing zone devices for infinitude-${idx}")
        	}
        }
        
        // add missing zones
        for ( i = zonesCount; i < systemConfig.zones[0].zone.size(); i++) {
            ifDebug("Processing zone ${i}")
            def zone = systemConfig.zones[0].zone[i]
            
            if (zone.enabled[0] == "on") {
                ifDebug("Adding zone ${i} ${zone.name[0]}")
                infinitudeSystem.createZone(zone, idx, i, systemStatus.status[idx].zones[0].zone[i], systemConfig)
            } else {
                ifDebug("Skipping zone ${i} as not enabled")
            }
		}
    }
}

String getFutureTimePlusMinutes(int minutes) {
    // Get the current local time (hours, minutes, seconds, nanos)
    LocalTime currentTime = LocalTime.now()

    // Add 60 minutes to the current time
    LocalTime futureTime = currentTime.plusMinutes(minutes)

    // Define the desired 24-hour format (HH for 00-23, mm for 00-59)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm")

    // Format the future time into the desired string
    return futureTime.format(formatter)
}

// Helper method to set up subscriptions
def subscribeToThermostatEvents(thermostatDevice) {
    if (!thermostatDevice) {
        log.warn "Cannot subscribe to events, thermostatDevice is null."
        return
    }
    
    ifDebug("Subscribing to events from ${thermostatDevice.displayName}")

    // Unsubscribe first to prevent duplicates if this method is called multiple times for the same device
    unsubscribe(thermostatDevice) // Unsubscribe from this specific device's previous subscriptions by this app

    subscribe(thermostatDevice, "thermostatMode", handleThermostatModeEvent)
    subscribe(thermostatDevice, "heatingSetpoint", handleHeatingSetpointEvent)
    subscribe(thermostatDevice, "coolingSetpoint", handleCoolingSetpointEvent)

    ifDebug("Parent App: Subscribed to events from child thermostat '${thermostatDevice.displayName}'")
}


// --- Event Handler Methods ---
// These will be called when the CHILD virtual thermostat's attributes change

void handleThermostatModeEvent(evt) {
    // IMPORTANT: Check the flag to prevent re-entry
    if (state.updatingFromPhysicalThermostat) {
        ifDebug("Skipping physical thermostat update: event triggered by app syncing from physical device.")
        return // Exit the handler early
    }
    
    ifDebug("CHILD Event: ${evt.displayName} - ${evt.name}: ${evt.value}")

    ifDebug("Child thermostat mode changed to: ${evt.value}. Parent app can now react.")
}

void handleHeatingSetpointEvent(evt) {
    // IMPORTANT: Check the flag to prevent re-entry
    if (state.updatingFromPhysicalThermostat) {
        ifDebug("Skipping physical thermostat update: event triggered by app syncing from physical device.")
        return // Exit the handler early
    }
    
    ifDebug("CHILD Event: ${evt.displayName} - ${evt.name}: ${evt.value} ${evt.unit}")
    BigDecimal newSetpoint = evt.value.toBigDecimal()
    
    ifDebug("Child thermostat heating setpoint changed to: ${newSetpoint} ${evt.unit}. Parent app can now react.")
}

void handleCoolingSetpointEvent(evt) {
    // IMPORTANT: Check the flag to prevent re-entry
    if (state.updatingFromPhysicalThermostat) {
        ifDebug("Skipping physical thermostat update: event triggered by app syncing from physical device.")
        return // Exit the handler early
    }
    
    ifDebug("CHILD Event: ${evt.displayName} - ${evt.name}: ${evt.value} ${evt.unit}")
    BigDecimal newSetpoint = evt.value.toBigDecimal()
    
    // get current status
    def systems = getSystems()
    
    // get device info
    def device = evt.device
    String deviceName = device.name
    String deviceDNI = device.deviceNetworkId
    Integer zoneId = getZoneFromName(deviceName)
    Integer systemId = getSystemFromName(deviceName)
    
    ifDebug("System ${systemId} Zone ${zoneId} Device ${deviceName}")
    
    // Update status object
    // update manual cooling set point (clsp)
    def activity = systems.system[systemId].config[0].zones[0].zone[zoneId].activities[0].activity
    ifDebug(activity)
    def manualIdx = findManualIdIndex(activity)
    
    systems.system[systemId].config[0].zones[0].zone[zoneId].activities[0].activity[manualIdx].clsp[0] = newSetpoint.toString()
    systems.system[systemId].config[0].zones[0].zone[zoneId].hold = ["on"]
    systems.system[systemId].config[0].zones[0].zone[zoneId].holdActivity = ["manual"]
    String holdTime = getFutureTimePlusMinutes(60)
	ifDebug("Home time set to ${holdTime}")
    systems.system[systemId].config[0].zones[0].zone[zoneId].otmr = [holdTime]
    updateSystems(systems)
    
    ifDebug("Child thermostat cooling setpoint changed to: ${newSetpoint} ${evt.unit}. Parent app can now react.")
}

int findManualIdIndex(List<Map<String, Object>> listOfMaps) {
    if (listOfMaps == null) {
        return -1
    }

    for (int i = 0; i < listOfMaps.size(); i++) {
        def item = listOfMaps[i]
        // Ensure the item is not null, has an 'id' key, and its value is "manual"
        if (item != null && item.containsKey('id') && item.id == 'manual') {
            return i
        }
    }
    return -1 // Return -1 if "manual" id is not found
}

def getZoneFromName(devName) {
    List<String> nameParts = devName.split('-')

    if (nameParts.size() == 3 && nameParts[0] == "infinitude") {
        Integer systemValue = nameParts[1].toInteger()
        Integer zoneValue = nameParts[2].toInteger()
        
        return zoneValue
    } else {
        log.warn "Could not parse system and zone from device name: ${devName}. Expected format 'infinitude-SYSTEM-ZONE'."
    }
}

def getSystemFromName(devName) {
    List<String> nameParts = devName.split('-')

    if (nameParts.size() == 3 && nameParts[0] == "infinitude") {
        Integer systemValue = nameParts[1].toInteger()
        Integer zoneValue = nameParts[2].toInteger()
        
        return systemValue
    } else {
        log.warn "Could not parse system and zone from device name: ${devName}. Expected format 'infinitude-SYSTEM-ZONE'."
    }
}

def GetServerIP ()
{
    return (settings.InfinitudeIP);
}

def GetServerPort ()
{
    return (settings.InfinitudePort);
}

private ifDebug(msg){
    if (msg && state.isDebug)  log.debug 'Infinitude Integration: ' + msg
}
