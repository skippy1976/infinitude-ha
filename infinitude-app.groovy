/*
 * Infinitude App
 *
 * Initial release: 
 */

import java.time.LocalTime
import java.time.format.DateTimeFormatter

def version() { return '0.0.1' }

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
    page(name: 'mainPage')
    page(name: 'systemsPage', nextPage: 'mainPage')
    page(name: 'editSytemPage', nextPage: 'systemsPage')
    page(name: 'aboutPage', nextPage: 'mainPage')
    page(name: 'configPage', nextPage: 'mainPage')
    page(name: 'modeMappingPage', nextPage: 'mainPage')
}

// ***** App Pages/Views *****

// Main Page
def mainPage() {
    ifDebug('Main page');

    state.isDebug = isDebug
    def systemCount = getChildDevices().size();
    ifDebug("System device count: ${systemCount}")
    
    if (!state.infinitudeIntegrationInstalled || systemCount == 0) {
        ifDebug('Initial main page')

        return dynamicPage(name: 'mainPage', title: 'Infinitude Ssytem Installation', install: true, uninstall: true) {        
            showTitle()
            section('<h1>Define your Infinitude system</h1>') {
                clearStateVariables()
                input name: 'InfinitudeIP', type: 'text', title: 'Infinitude Server IP Address', required: true, submitOnChange: true
                input name: 'InfinitudePort', type: 'text', title: 'Infinitude Server Port Number',  defaultValue: '3000', required: true, submitOnChange: true
                input name: 'enableDebug', type: 'bool', title: 'Enable debug logging', defaultValue: true, submitOnChange: true
            }
        }
    } else {
        ifDebug('Initialized main page')
        
        return dynamicPage(name: 'mainPage', title: 'Infinitude Ssytem Settings', install: true, uninstall: true) {
            showTitle()
            
            section('<h1>Systems</h1>') {
                href (name: 'systemsPage',
                      title: 'Systems',
                      description: 'Add names for your systems',
                      page: 'systemsPage')
                paragraph "Navigate to your <a href='http://${InfinitudeIP}:${InfinitudePort}/'>Infinitude site http://${InfinitudeIP}:${InfinitudePort}/</a>."
            }

            section('<h1>About</h1>') {
                href (name: 'aboutPage',
                      title: 'About',
                      description: 'Find out more about Infinitude Integration',
                      page: 'aboutPage')
            }

            section('<h1>Configuration</h1>') {
                href (name: 'configPage',
                      title: 'Configuration',
                      description: 'Change the IP address and port number of Infinitude as well as enable debug',
                      page: 'configPage')
            }

            section('<h1>Mode Mapping</h1>') {
                href (name: 'modeMappingPage',
                      title: 'Mode Mapping',
                      description: 'Map the Hubitat modes to the Infinitude thermostat modes',
                      page: 'modeMappingPage')
            }
        }
    }
}

// About page
def aboutPage() {
    ifDebug('Showing aboutPage')

    dynamicPage(name: 'aboutPage', title: none){
        section('<h1>Introducing Infinitude Integration</h1>') {
            paragraph 'A module that provide access to Infinitude...' +
                ' in turn providing access to your Carrier Infinity or Bryant Evolution HVAC system.'
            paragraph 'You must already have Infinitude setup and operational with your Infinity / Evolution ssytem.'
            paragraph 'More information about the Infinitude project and how to get started can be found on <a href="https://github.com/nebulous/infinitude">GitHub</a>.'
        }
    }
}

// Config page
def configPage() {
    ifDebug('Showing configPage')

    dynamicPage(name: 'configPage', title: none){
        section('<h1>Infinitude Configuration</h1>') {
            input name: 'InfinitudeIP', type: 'text', title: 'Infinitude Server IP Address', required: true, submitOnChange: true
            input name: 'InfinitudePort', type: 'text', title: 'Infinitude Server Port Number',  defaultValue: '3000', required: true, submitOnChange: true
            input name: 'isDebug', type: 'bool', title: 'Enable debug logging', defaultValue: true, submitOnChange: true
        }
    }
}

// mode mapping page
def modeMappingPage() {
    ifDebug('Showing modeMappingPage')

    dynamicPage(name: 'modeMappingPage', title: none){
        // This new section dynamically creates settings for each Hubitat location mode.
        section('Mode Mapping') {
            if (location.modes) {
                location.modes.each { mode ->
                    input(name: "mode_${mode.name.replaceAll(/\s+/, "")}", type: "enum", title: "When in '${mode.name}' mode, set Infinitude to:", options: ["Home", "Away"], required: false)
                }
            } else {
                paragraph "Could not retrieve location modes. Please ensure modes are configured in your Hubitat settings."
            }
        }        
    }
}

// Page for editing systems names
def systemsPage() {
    ifDebug('Showing systemsPage')
    
    def systemDevice = getChildDevice()
    ifDebug('getChildDevice: ${childDevices}')
    
    if (getChildDevices().size() == 0 && !state.infinitudeIntegrationInstalled)
    {
        log.warn ('not initialised')
        return
    }
    
    if (state.editingSystem)
    {
        editSystem()
    }

    dynamicPage(name: 'systemsPage', title: '', install: false, uninstall: false){

        section('<h1>Systems</h1>'){
            paragraph 'Potentially your home may consist of multiple seperate Infinity or Evolution systems' + 
                'Note that these are different and should not be confused with zones. Each system may consist of multiple zones.'
            paragraph 'You will want to provide a name for each system to ensure that it is something meaningful. ' +
                'The system name is the parent device in the Hubitat devices that will have all the zone thermostat devices.'

            getChildDevices().each{
                href (name: 'editSytemPage', title: "${it.label} (${it.name})",
                      description: "System Details\t${it.getTypeName()}",
                      params: [deviceNetworkId: it.deviceNetworkId],
                      page: 'editSytemPage')
            }
        }
    }
}

// Edit system page
def editSytemPage(message) {
    ifDebug('Showing editSystemPage')
    ifDebug("editing ${message.deviceNetworkId}")
    
    state.editingSystem = true
    state.editedDeviceNetworkId = message.deviceNetworkId;
    def systemDevice = getChildDevice(message.deviceNetworkId)
    
    ifDebug("editing systemDevice: name = ${systemDevice.name} label = ${systemDevice.label}")
    
    dynamicPage(name: 'editSytemPage', title: ''){
        section('<h1>Edit an Infinitude System</h1>'){
            paragraph "${systemDevice.label}"
            paragraph paragraphText
            paragraph ''
            input 'newSystemLabel', 'text', title: 'System Name', required: true, multiple: false, defaultValue: systemDevice.name, submitOnChange: false
        }
    }
}

// Method to update the system
def editSystem(){
    ifDebug('editSystem updating')
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
    def infinitudeSystemDriverVersion = (getInfinitudeSystemDevice(0) == null) ? 'Not Yet Installed' : getInfinitudeSystemDevice(0).version()
    def infinitudeZoneDriverVersion = (getInfinitudeZoneDevice(0,0) == null) ? 'Not Yet Installed' : getInfinitudeZoneDevice(0,0).version()
    section('<h1>Versions</h1>') {
        paragraph "<br>App: [${appVersion}]<br>Infinitude System Driver [${infinitudeSystemDriverVersion}]<br>Infinitude Zone Driver [${infinitudeZoneDriverVersion}]"
    }
    
}

// ***** Hubitat System Methods *****

// called when app is installed
void installed()
{
    ifDebug('Installed')
    state.infinitudeIntegrationInstalled = true

    unsubscribe()
    updated()
}

// Called when uninstalling
void uninstalled()
{
    clearStateVariables()
    // unsubscribe to all events
    ifDebug('Uninstalled')
    removeChildDevices(getChildDevices())
    unsubscribe()
}

// initialized
def initialize() {
    ifDebug('initialize')
    // Subscribing to location mode changes.
    // This ensures modeUpdateHandler is called whenever the location mode changes.
    subscribe(location, "mode", modeUpdateHandler)
}

// called following update
void updated() {
    ifDebug("Updated...")
    unsubscribe()
    initialize()
    pollInfinitude()

    // After settings are saved, immediately apply the mapping for the current mode.
    if (location.currentMode) {
        ifDebug("Syncing current mode '${location.currentMode}' based on new settings.")
        modeUpdateHandler([value: location.currentMode])
    }
}

// ***** Custom Methods *****

// Handles HE location mode changes.
def modeUpdateHandler(evt) {
    ifDebug("Location mode changed to '${evt.value}'.")
    // Sanitize the mode name to match the setting name (e.g., "Away Mode" -> "mode_AwayMode").
    def settingName = "mode_${evt.value.toString().replaceAll(/\s+/, "")}"
    def infinitudeMode = settings[settingName]

    if (infinitudeMode) {
        ifDebug("Mapping '${evt.value}' to Infinitude mode '${infinitudeMode}'. Forwarding to parent.")
        // This assumes your parent app has a method to set the mode on the physical system.
        parent?.setInfinitudeSystemMode(infinitudeMode)
    } else {
        ifDebug("No mapping found for Hubitat mode '${evt.value}'. No action taken.")
    }
}

// clear settings variables
def clearStateVariables(){
    ifDebug('Clearing Settings Variables just in case.')
    settings.InfinitudeIP = null
    settings.InfinitudePort = null
    settings.logEnable = null
}

// method to retrieve the Infinitude Zone device based on system index and Zone index (indexed to 0)
def getInfinitudeZoneDevice(idx, zoneIdx){
    def name = "infinitude-${idx}-${zoneIdx}"
    ifDebug("getInfinitudeZoneDevice ${name}")
    def infinitudeSystem = getInfinitudeSystemDevice(idx)
    if (infinitudeSystem == null) {
        return null
    }

    def infinitudeZone = infinitudeSystem.getChildDevice(name)
    return infinitudeZone
}

// method to retrieve the Infinitude System device based on system index (indexed to 0)
def getInfinitudeSystemDevice(idx){
    def name = "infinitude-${idx}"
    ifDebug("getInfinitudeSystemDevice ${name}")
    def infinitudeSystem = getChildDevice(name)
    return infinitudeSystem
}

// TODO:  not sure if still used
void eventHandler(evt)
{
    ifDebug('event received')
}

// remove all child devices
private removeChildDevices(delete) {
    delete.each {deleteChildDevice(it.deviceNetworkId)}
}

// Create a parent infinitude system device
def createInfinitudeSystem(systemId){    
    // check if it already exists
    def infinitudeSystem = getInfinitudeSystemDevice(systemId)
    if (infinitudeSystem == null){
        ifDebug("Creating System Device ${systemId} infinitude-${systemId}")
        addChildDevice('Infinitude', 'Infinitude System', "infinitude-${systemId}", null, [name: "Infinitude Ssytem ${systemId}", isComponent: true, label: "Infinitude Ssytem ${systemId}"])
        return getInfinitudeSystemDevice(systemId)
    } else {
        ifDebug("Infinitude system with ID ${systemId} already exists")
        return infinitudeSystem
    }
}

String getFutureTimePlusMinutes(int minutes) {
    // Get the current local time (hours, minutes, seconds, nanos)
    LocalTime currentTime = LocalTime.now()

    // Add 60 minutes to the current time
    LocalTime futureTime = currentTime.plusMinutes(minutes)

    // Define the desired 24-hour format (HH for 00-23, mm for 00-59)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern('HH:mm')

    // Format the future time into the desired string
    return futureTime.format(formatter)
}

// ***** Methods for integrating with Infinitude *****

// perform http get from Infinitude
def httpGetExec(webserviceurl)
{
    ifDebug('httpGetExec()')
    try {
        def params = [
           uri: webserviceurl,
           contentType: 'application/json'
        ]
        
        httpGet(params) { resp ->
            if (resp.data) {
                return resp.data
            }
        }
    }
    catch (Exception e) {
        log.warn("httpGetExec() failed: ${e.message}")
    }
}

// Method to retrive the systems from Infinitude
def getSystems() {
    String s = "http://${settings.InfinitudeIP}:${settings.InfinitudePort}/systems.json"
    ifDebug("Infinitude request ${s}")
    def systems = httpGetExec(s)   
}

// Method to update the systems from Infinitude
def updateSystems(systems) {
    String s = "http://${settings.InfinitudeIP}:${settings.InfinitudePort}"
    ifDebug("Infinitude request ${s}")
    httpPost(uri: s,
                 path: '/systems/infinitude',
                 contentType: 'application/json;charset=UTF-8',
                 requestContentType: 'application/json;charset=UTF-8',
                 body: systems
                ) { response ->
             
             ifDebug("hub remote id: ${response.status}")
             return
        }
}
    
// Method to retrive the system status from Infinitude
def getSystemStatus() {
    String s = "http://${settings.InfinitudeIP}:${settings.InfinitudePort}/status.json"
    ifDebug("Infinitude request ${s}")
    def systemStatus = httpGetExec(s)   
}

// Connect to Infinitude to get update
void pollInfinitude() {
    ifDebug("Polling Infinitude...")
    state.infinitudeIntegrationInstalled = true
    ifDebug("Updated [${settings.InfinitudeIP}]:[${settings.InfinitudePort}]")

    settings.InfinitudrIP = settings.InfinitudeIP.trim()
    settings.InfinitudrPort = settings.InfinitudePort.trim()

    def systems = getSystems()
    def systemStatus = getSystemStatus()
    
    int systemsSize = systems.size()

    if (systemsSize == 0) {
        log.warn('systems is empty list')
        return
    } else {
        ifDebug("Infinitude systems ${systemsSize}")
    }
    
    // user may have multiple systems, operate on each system
    systems.system.eachWithIndex { system, idx ->
        ifDebug("Infinitude system version: ${system.version}")

        // Create each system
        def infinitudeSystem = createInfinitudeSystem(idx)

        infinitudeSystem.updateSystem(system.config[0], systemStatus.status[idx], idx)
    }
}

// Helper method to set up subscriptions
def subscribeToThermostatEvents(thermostatDevice) {
    if (!thermostatDevice) {
        log.warn('Cannot subscribe to events, thermostatDevice is null.')
        return
    }
    
    ifDebug("Subscribing to events from ${thermostatDevice.displayName}")

    // Unsubscribe first to prevent duplicates if this method is called multiple times for the same device
    unsubscribe(thermostatDevice) // Unsubscribe from this specific device's previous subscriptions by this app

    subscribe(thermostatDevice, 'thermostatMode', handleThermostatModeEvent)
    subscribe(thermostatDevice, 'heatingSetpoint', handleHeatingSetpointEvent)
    subscribe(thermostatDevice, 'coolingSetpoint', handleCoolingSetpointEvent)

    ifDebug("Parent App: Subscribed to events from child thermostat ${thermostatDevice.displayName}")
}


// ***** Event Handler Methods *****

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

        // IMPORTANT: Add a check to ensure 'item' is actually a Map
        // before trying to access its properties like 'id'
        if (item instanceof Map) {
            if (item.containsKey('id') && item.id == 'manual') {
                return i
            }
        } else {
            // Log a warning if an unexpected object type is found in the list
            log.warn "Infinitude Integration: findManualIdIndex: Unexpected object type found in activities list at index"
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

// Helper method for debug logging.
private ifDebug(msg) {
    if (enableDebug) {
        log.debug "${msg}"
    }
}
