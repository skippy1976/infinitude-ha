/*
 * Infinitude App
 *
 * Initial release: 
 */

def version() { return "Infinitude Hubitat App 0.0.1" }

definition(
    name: "Infinitude Integration",
    namespace: "Infinitude",
    author: "skippy76",
    description: "Creates virtual devices for Carrier / Bryant thermostats.",
    category: "Convenience",
    importUrl: "",
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
                input "InfinitudeIP", "text", title: "Infinitude Server IP Address", required: true, submitOnChange: true
                input "InfinitudePort", "text", title: "Infinitude Server Port Number",  defaultValue: "3000", required: true, submitOnChange: true
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
    
    updated()
}

// Called when uninstalling
void uninstalled()
{
    // unsubscribe to all events
    ifDebug("Uninstalled")
    removeChildDevices(getChildDevices())
    unsubscribe()
}

void eventHandler(evt)
{
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
        return addChildDevice("Infinitude", "Infinitude System", "infinitude-${systemId}", null, [name: "Infinitude Ssytem ${systemId}", isComponent: true, label: "Infinitude Ssytem ${systemId}"])
    } else {
        log.warn("Infinitude system with ID ${systemId} already exists")
        return getInfinitudeSystemDevice(systemId)
    }
}

// called following update
void updated()
{
    ifDebug("In Updated [${settings.InfinitudeIP}]")
    ifDebug("In Updated [${settings.InfinitudePort}]")
    ifDebug("In Updated [${settings.DoGet}]")

    settings.InfinitudrIP = settings.InfinitudeIP.trim()
    settings.InfinitudrPort = settings.InfinitudePort.trim()

    String s = "http://" + settings.InfinitudeIP + ":" + settings.InfinitudePort + "/systems.json"
    ifDebug("Infinitude request ${s}")
    def systems = httpGetExec(s)
    
    s = "http://" + settings.InfinitudeIP + ":" + settings.InfinitudePort + "/status.json"
    ifDebug("Infinitude request ${s}")
    def systemStatus = httpGetExec(s)
    
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
