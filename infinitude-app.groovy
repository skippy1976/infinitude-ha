/*
 * Infinitude App
 *
 * Initial release: 0.0.1
 * Revised: 0.0.2
 */

import groovy.json.JsonSlurper
import java.time.LocalTime
import java.time.format.DateTimeFormatter

def version() { return '0.0.2' }

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

def mainPage() {
    ifDebug('Main page')

    def systemDevices = getChildDevices()
    ifDebug(systemDevices)
    // If IP or Port is not set, force the initial setup page.
    if ((!InfinitudeIP || !InfinitudePort) || (systemDevices == [])) {
        return dynamicPage(name: 'mainPage', title: 'Infinitude System Installation', install: true, uninstall: true) {
            showTitle()
            section('<h1>Define your Infinitude system</h1>') {
                paragraph "Please enter the IP Address and Port for your Infinitude server."
                input name: 'InfinitudeIP', type: 'text', title: 'Infinitude Server IP Address', required: true
                input name: 'InfinitudePort', type: 'text', title: 'Infinitude Server Port Number', defaultValue: '3000', required: true
                input name: 'enableDebug', type: 'bool', title: 'Enable debug logging', defaultValue: true
            }
        }
    } else {
        // Normal main page for installed app.
        return dynamicPage(name: 'mainPage', title: 'Infinitude System Settings', install: false, uninstall: true) {
            showTitle()
            
            section('<h1>Systems</h1>') {
                href (name: 'systemsPage', title: 'Systems', description: 'Add names for your systems', page: 'systemsPage')
                paragraph "Navigate to your <a href='http://${InfinitudeIP}:${InfinitudePort}/' target='_blank'>Infinitude site</a>."
            }

            section('<h1>Settings</h1>') {
                 href (name: 'modeMappingPage', title: 'Mode Mapping', description: 'Map Hubitat modes to Infinitude modes', page: 'modeMappingPage')
                 href (name: 'configPage', title: 'Configuration', description: 'Change Infinitude connection settings', page: 'configPage')
            }

            section('<h1>About</h1>') {
                href (name: 'aboutPage', title: 'About', description: 'Find out more about Infinitude Integration', page: 'aboutPage')
            }
        }
    }
}

def aboutPage() {
    ifDebug('Showing aboutPage')
    dynamicPage(name: 'aboutPage', title: 'About Infinitude Integration'){
        section {
            paragraph 'A module that provides access to Infinitude, which in turn provides access to your Carrier Infinity or Bryant Evolution HVAC system.'
            paragraph 'You must already have Infinitude setup and operational with your Infinity / Evolution system.'
            paragraph 'More information about the Infinitude project can be found on <a href="https://github.com/nebulous/infinitude" target="_blank">GitHub</a>.'
        }
    }
}

def configPage() {
    ifDebug('Showing configPage')
    dynamicPage(name: 'configPage', title: 'Configuration'){
        section('<h1>Infinitude Configuration</h1>') {
            input name: 'InfinitudeIP', type: 'text', title: 'Infinitude Server IP Address', required: true
            input name: 'InfinitudePort', type: 'text', title: 'Infinitude Server Port Number', defaultValue: '3000', required: true
            input name: 'enableDebug', type: 'bool', title: 'Enable debug logging', defaultValue: true
        }
    }
}

def modeMappingPage() {
    ifDebug('Showing modeMappingPage')
    dynamicPage(name: 'modeMappingPage', title: 'Mode Mapping'){
        section {
            if (location.modes) {
                location.modes.each { mode ->
                    input(name: "mode_${mode.name.replaceAll(/\s+/, "")}", type: "enum", title: "When in '${mode.name}' mode, set Infinitude to:", options: ["Home", "Away", "Sleep", "Awake"], required: false)
                }
            } else {
                paragraph "Could not retrieve location modes. Please ensure modes are configured in your Hubitat settings."
            }
        }
    }
}

def systemsPage() {
    ifDebug('Showing systemsPage')
    
    if (state.editingSystem) {
        editSystem()
    }

    dynamicPage(name: 'systemsPage', title: 'Systems'){
        section {
            paragraph 'Provide a meaningful name for each system. The system name is the parent device in Hubitat that will contain all the zone thermostat devices.'
            getChildDevices().each {
                href (name: 'editSytemPage', title: "${it.label} (${it.name})",
                      description: "Edit System Name",
                      params: [deviceNetworkId: it.deviceNetworkId],
                      page: 'editSytemPage')
            }
        }
    }
}

def editSytemPage(params) {
    ifDebug("Showing editSystemPage for ${params.deviceNetworkId}")
    
    state.editingSystem = true
    state.editedDeviceNetworkId = params.deviceNetworkId
    def systemDevice = getChildDevice(params.deviceNetworkId)
    
    dynamicPage(name: 'editSytemPage', title: "Edit System: ${systemDevice.label}"){
        section {
            input 'newSystemLabel', 'text', title: 'New System Name', required: true, defaultValue: systemDevice.label
        }
    }
}

private editSystem() {
    ifDebug("Updating system label for ${state.editedDeviceNetworkId}")
    def childSystem = getChildDevice(state.editedDeviceNetworkId)

    if (childSystem && newSystemLabel) {
        ifDebug("Renaming system from '${childSystem.label()}' to '${newSystemLabel}'")
        childSystem.setLabel(newSystemLabel)
    }
    
    state.editingSystem = false
    state.editedDeviceNetworkId = null
}

private showTitle() {
    section('<h1>Information</h1>') {
        def infinitudeSystemDriverVersion = (getInfinitudeSystemDevice(0) == null) ? 'Not Yet Installed' : getInfinitudeSystemDevice(0).version()
        def infinitudeZoneDriverVersion = (getInfinitudeZoneDevice(0,0) == null) ? 'Not Yet Installed' : getInfinitudeZoneDevice(0,0).version()
        paragraph "<b>App Version:</b> [${version()}]<br>"
        paragraph "<b>Infinitude System Driver Version:</b> [${infinitudeSystemDriverVersion}]<br>"
        paragraph "<b>Infinitude Zone Driver Version:</b> [${infinitudeZoneDriverVersion}]"
    }
}

// ***** Hubitat System Methods *****

void installed() {
    ifDebug('Installed')
    initialize()
}

void uninstalled() {
    ifDebug('Uninstalled')
    removeChildDevices(getChildDevices())
    unsubscribe()
}

void updated() {
    ifDebug("Settings updated...")
    unsubscribe()
    initialize()
}

void initialize() {
    ifDebug('Initializing...')
    if (!InfinitudeIP || !InfinitudePort) {
        log.warn "Infinitude IP Address or Port not set. Please configure in the app."
        return
    }

    // Perform an initial poll to create devices.
    pollInfinitude()
}

// ***** Custom Methods *****

// clear settings variables
def clearStateVariables(){
    ifDebug('Clearing Settings Variables just in case.')
    settings.InfinitudeIP = null
    settings.InfinitudePort = null
    settings.logEnable = null
}

// method to retrieve the Infinitude System device based on system index (indexed to 0)
def getInfinitudeSystemDevice(idx){
    def name = "infinitude-${idx}"
    ifDebug("getInfinitudeSystemDevice ${name}")
    def infinitudeSystem = getChildDevice(name)
    return infinitudeSystem
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

private removeChildDevices(delete) {
    delete.each { deleteChildDevice(it.deviceNetworkId) }
}

private createInfinitudeSystem(systemId) {
    def dni = "infinitude-${systemId}"
    def existingDevice = getChildDevice(dni)
    if (!existingDevice) {
        ifDebug("Creating System Device: ${dni}")
        def defaultLabel = "Infinitude System ${systemId + 1}"
        return addChildDevice('Infinitude', 'Infinitude System', dni, [name: defaultLabel, isComponent: false, label: defaultLabel])
    } else {
        return existingDevice
    }
}

// ***** Methods for integrating with Infinitude *****

private httpGetExec(path) {
    def url = "http://${InfinitudeIP}:${InfinitudePort}/${path}"
    ifDebug("HTTP GET: ${url}")
    try {
        httpGet(uri: url, contentType: 'application/json') { resp ->
            if (resp.status == 200 && resp.data) {
                return resp.data
            } else {
                log.error "HTTP GET failed: ${resp.status} - ${resp.errorMessage}"
                return null
            }
        }
    } catch (Exception e) {
        log.error "httpGetExec() failed for ${url}: ${e.message}"
        return null
    }
}

private httpPostExec(path, body) {
    def url = "http://${InfinitudeIP}:${InfinitudePort}/${path}"
    ifDebug("HTTP POST: ${url}")
    try {
        httpPostJson(uri: url, body: body) { resp ->
            if (resp.status == 200) {
                ifDebug("HTTP POST successful.")
            } else {
                log.error "HTTP POST failed: ${resp.status} - ${resp.errorMessage}"
            }
        }
    } catch (Exception e) {
        log.error "httpPostExec() failed for ${url}: ${e.message}"
    }
}

private getSystems() {
    return httpGetExec('systems.json')
}

private updateSystems(systems) {
    // Infinitude API expects the update on a specific path.
    return httpPostExec('systems/infinitude', systems)
}

private getSystemStatus() {
    return httpGetExec('status.json')
}

void pollInfinitude() {
    ifDebug("Polling Infinitude...")
    
    def systems = getSystems()
    def systemStatus = getSystemStatus()
    
    if (!systems?.system || !systemStatus?.status) {
        log.warn('Could not retrieve valid data from Infinitude. Check connection and server status.')
        return
    }
    
    systems.system.eachWithIndex { systemConfig, idx ->
        def infinitudeSystem = createInfinitudeSystem(idx)
        def currentStatus = systemStatus.status[idx]
        infinitudeSystem.updateSystem(systemConfig.config[0], currentStatus, idx)
    }
}

// ***** Helper Methods *****

private ifDebug(msg) {
    if (enableDebug) {
        log.debug "Infinitude App: ${msg}"
    }
}
