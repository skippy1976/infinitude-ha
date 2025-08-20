/*
 * Infinitude System driver
 *
 * Initial release: 0.0.1
 * Revised: 0.0.2
 */

import groovy.json.JsonOutput

// A version function is good practice for tracking updates.
def version() { return '0.0.2' }

metadata {
    definition(name: 'Infinitude System',
               namespace: 'Infinitude',
               author: 'skippy76',
               importUrl: 'https://raw.githubusercontent.com/skippy1976/infinitude-ha/refs/heads/main/infinitude-driver.groovy'
              ) {
        // Capabilities define the standard functions your device supports.
        capability "Polling"
        capability "Initialize"
        capability "Refresh"
        capability "RelativeHumidityMeasurement"
        capability "ThermostatSchedule"

        // Custom attributes for data not covered by standard capabilities.
        attribute "Airflow", "STRING"
        attribute "FilterUsage", "STRING"
        attribute "VentUsage", "STRING"
        attribute "OutsideTemperature", "STRING"
        attribute "TemperatureUnit", "STRING"
        attribute "OperatingMode", "ENUM", ['dehumidify','cool','heat','off']
        attribute "ConfigurationType", "ENUM", ['heatcool','heat','cool']
        attribute "VacationRunning", "STRING"
        attribute "SystemVersion", "STRING"
        attribute "UVLevel", "INTEGER"
        attribute "HumidifierActive", "STRING"
        attribute "SystemIndex", "NUMBER"
    }

    preferences {
        section('Infinitude Connection') {
            // Using a map for poll rate options is clean and effective.
            def pollRateOptions = ['0' : 'Disabled', '1' : 'Poll every minute', '5' : 'Poll every 5 minutes', '10' : 'Poll every 10 minutes', '15' : 'Poll every 15 minutes', '30' : 'Poll every 30 minutes']
            input(name: 'pollRate', type: 'enum', title: 'Device Poll Rate', options: pollRateOptions, defaultValue: '5', required: true)
            input(name: 'holdDuration', type: 'integer', title: 'Hold duration in minutes', defaultValue: 60, required: true)
            input(name: 'enableDebug', type: 'bool', title: 'Enable debug logging', defaultValue: true)
        }
    }
}

// ***** Hubitat System Methods *****

def installed() {
    ifDebug('Infinitude driver Installed...')
}

def uninstalled() {
    ifDebug('Infinitude driver Uninstalled...')
}

def updated() {
    ifDebug('Updated settings...')

    // Unschedule all previous polling to prevent orphaned schedules when the rate is changed.
    unschedule()

    // It's good practice to re-initialize on update.
    initialize()

    // Schedule polling based on user preference.
    if (pollRate != '0') {
        def pollMinutes = pollRate.toInteger()
        ifDebug("Scheduling poll for every ${pollMinutes} minute(s).")
        // Hubitat has built-in methods for scheduling every X minutes.
        switch (pollMinutes) {
            case 1: runEvery1Minute(poll); break
            case 5: runEvery5Minutes(poll); break
            case 10: runEvery10Minutes(poll); break
            case 15: runEvery15Minutes(poll); break
            case 30: runEvery30Minutes(poll); break
            default: ifDebug("Invalid poll rate: ${pollRate}"); break
        }
    } else {
        ifDebug('Polling is disabled.')
    }
}

def subscribe() {
    ifDebug('Subscribe method.')
}

String label() {
    ifDebug('Label method.')
    return getName()
}

// ***** Capability Command Methods *****

def initialize() {
    ifDebug('Initializing...')
    // This driver appears to be a child of a parent App or Device.
    // The 'parent' object must be present for this driver to function.
    if (!parent) {
        log.warn "This driver must be used as a child of a parent application."
        return
    }

    // Subscribe to location mode changes to trigger mode mapping.
    subscribe(location, "mode", modeUpdateHandler)
}

def refresh() {
    ifDebug('Refresh command received. Polling parent for new data...')
    // The refresh command should trigger a data fetch from the parent.
    parent?.pollInfinitude()
}

def poll() {
    ifDebug('Scheduled poll running. Polling parent for new data...')
    // The poll method should trigger the data fetch from the parent.
    parent?.pollInfinitude()
}

def call() {
    ifDebug('Call called...')
}

// ***** Custom Methods *****

// Main method to update the system and its child zones from parent data.
def updateSystem(systemConfig, systemStatus, idx) {
    ifDebug("Updating system 'infinitude-${idx}' with new data.")
    ifDebug("systemConfig: ${JsonOutput.toJson(systemConfig)}")
    ifDebug("systemStatus: ${JsonOutput.toJson(systemStatus)}")

    // Use Groovy's safe navigation operator (?.) to prevent NullPointerExceptions
    // if the data structure is not what's expected.
    def firstZoneStatus = systemStatus.zones[0].zone[0]

    if (firstZoneStatus) {
        sendEvent(name: "humidity", value: firstZoneStatus.rh[0], unit: "%")
    }
    if (systemStatus.idu[0]) {
        sendEvent(name: "Airflow", value: systemStatus.idu[0].cfm[0], unit: "CFM")
    }
    if (systemStatus.filtrlvl[0]) {
        sendEvent(name: "FilterUsage", value: systemStatus.filtrlvl[0], unit: "%")
    }
    if (systemStatus.ventlvl[0]) {
        sendEvent(name: "VentUsage", value: systemStatus.ventlvl[0], unit: "%")
    }
    if (systemStatus.mode[0]) {
        sendEvent(name: "OperatingMode", value: systemStatus.mode[0])
    }
    if (systemStatus.cfgtype[0]) {
        sendEvent(name: "ConfigurationType", value: systemStatus.cfgtype[0])
    }
    if (systemStatus.vacatrunning[0]) {
        sendEvent(name: "VacationRunning", value: systemStatus.vacatrunning[0])
    }
    if (systemStatus.version[0]) {
        sendEvent(name: "SystemVersion", value: systemStatus.version)
    }
    if (systemStatus.uvlvl[0]) {
        sendEvent(name: "UVLevel", value: systemStatus.uvlvl[0])
    }
    if (systemStatus.humid[0]) {
        sendEvent(name: "HumidifierActive", value: systemStatus.humid[0])
    }
    if (systemStatus.oat[0] && systemStatus.cfgem[0]) {
        def tempUnit = systemStatus.cfgem[0] == 'F' ? '°F' : '°C'
        sendEvent(name: "OutsideTemperature", value: "${systemStatus.oat[0]} ${tempUnit}")
        sendEvent(name: "TemperatureUnit", value: systemStatus.cfgem[0])
    }
    sendEvent(name: "SystemIndex", value: idx)

    // Synchronize child zone devices
    syncZones(systemConfig, systemStatus, idx)
}

private syncZones(systemConfig, systemStatus, idx) {
    def zonesFromApi = systemConfig.zones[0].zone ?: []
    def existingZones = getChildDevices()
    def apiZoneDnis = []

    // Update existing zones and create a list of DNIs from the API data.
    zonesFromApi.eachWithIndex { zoneConfig, zoneIdx ->
        def dni = "infinitude-${idx}-${zoneIdx}"
        apiZoneDnis << dni

        if (zoneConfig?.enabled[0] == "on") {
            ifDebug(existingZones)
            def zoneDevice = existingZones.find { it.deviceNetworkId == dni }
            def zoneStatus = systemStatus.zones[0].zone[zoneIdx]

            if (!zoneStatus) {
                ifDebug("No status data for zone ${dni}. Skipping update.")
                return
            }

            if (zoneDevice != null) {
                ifDebug("${dni} already exists")
                // Zone already exists, update it.
                zoneDevice = getChildDevice(dni)
                zoneDevice.updateZone(zoneConfig, zoneStatus, systemConfig)
            } else {
                ifDebug("${dni} does not exist, create")
                // Zone is new, create it.
                createZone(zoneConfig, zoneStatus, systemConfig, idx, zoneIdx)
            }
            apiZoneDnis.removeElement(dni)
        }
    }
    
    // Remove any child devices that are no longer present in the API config.
    existingZones.each { zoneDevice ->
        if (apiZoneDnis.contains(zoneDevice.deviceNetworkId)) {
            ifDebug("Removing stale zone: ${zoneDevice.label} (${zoneDevice.deviceNetworkId})")
            deleteChildDevice(zoneDevice.deviceNetworkId)
        }
    }
}

// Creates a new zone child device.
private createZone(zoneConfig, zoneStatus, systemConfig, systemIdx, zoneIdx) {
    def dni = "infinitude-${systemIdx}-${zoneIdx}"
    def zoneName = zoneConfig?.name[0] ?: "Zone ${zoneIdx + 1}"

    ifDebug("Creating new zone: '${zoneName}' with DNI: ${dni}")
    try {
        def newZone = addChildDevice('Infinitude', 'Infinitude Zone', dni, [name: dni, isComponent: false, label: zoneName])

        // set the thermostat zone index
        newZone.sendEvent(name: "ZoneIndex", value: zoneIdx)
        // Set the supported modes for the virtual thermostat. No need to convert to JSON.
        def fanModes = ["auto", "low", "medium", "high"]
        newZone.sendEvent(name: "SupportedThermostatFanModes", value: fanModes)
        def thermostatModes = ["off", "fan only", "auto", "heat", "cool"]
        newZone.sendEvent(name: "SupportedThermostatModes", value: thermostatModes)

        // Perform the initial update of the new device's state.
        newZone.updateZone(zoneConfig, zoneStatus, systemConfig)
    } catch (e) {
        log.error "Error creating zone device ${dni}: ${e.message}"
    }
}

void modeUpdateHandler(evt) {
    ifDebug("Location mode changed to '${evt.value}'.")
    def settingName = "mode_${evt.value.replaceAll(/\s+/, "")}"
    def infinitudeMode = parent.getSetting(settingName)

    if (infinitudeMode) {
        ifDebug("Mapping '${evt.value}' to Infinitude mode '${infinitudeMode}'.")
        setInfinitudeSystemMode(infinitudeMode)
    } else {
        ifDebug("No mapping found for Hubitat mode '${evt.value}'. No action taken.")
    }
}

// This method actually sends the mode change to the Infinitude server.
void setInfinitudeSystemMode(String mode) {
    def systemIndex = device.currentValue("SystemIndex").intValue()
    ifDebug("Attempting to set Infinitude system mode to '${mode}' for system index ${systemIndex}")
    def systems = getSystems()
    if (!systems) {
        log.error "Could not retrieve systems to set mode."
        return
    }

    if (mode == "fan only") { mode = "fanonly" }

    systems.system[systemIndex].config[0].mode[0] = [mode.toLowerCase()]

    // Post the entire modified configuration back.
    parent.updateSystems(systems)
}

def getModeMapping(settingName) {
    return parent.getSetting(settingName)
}

def getSystems() {
    return parent.getSystems()
}

def updateSystems(systems) {
    parent.updateSystems(systems)
}

// Helper method for debug logging.
private ifDebug(msg) {
    if (enableDebug) {
        // The log prefix helps identify messages from this specific device instance.
        log.debug "${device.displayName}: ${msg}"
    }
}
