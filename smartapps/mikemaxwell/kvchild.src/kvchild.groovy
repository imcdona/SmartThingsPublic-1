/**
 *  kvChild 0.0.6
 	
    0.0.6	added options on what to do when the zone is disabled...
    0.0.5	added disable switch option
    0.0.4	basic reporting
    0.0.3 	added dynamic zone change support while system is running
    		added support for main set point updates while system is running
    0.0.2	added F/C unit detection and display
    
 *  Copyright 2015 Mike Maxwell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
definition(
    name: "kvChild",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "child application for 'Keen Vent Manager', do not install directly.",
    category: "My Apps",
    parent: "MikeMaxwell:kvParent",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "main")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	//initialize()
}
def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(tempSensors, "temperature", ventHandler)
    subscribe(vents, "pressure", getAdjustedPressure)
    subscribe(vents, "temperature", getAdjustedPressure)
    //subscribe(zoneControlSwitch,"switch",
    //capability.switch
}

//dynamic page methods
def main(){
	def installed = app.installationState == "COMPLETE"
    def soc = (state.hvacMode ?: "idle") != "idle"
    //log.info "main page submitOnChange:${soc} state.running:${state.running} state.hvacMode:${state.hvacMode}"
	return dynamicPage(
    	name		: "main"
        ,title		: "Zone Configuration"
        ,install	: true
        ,uninstall	: installed
        ){
		     section("Zone Devices"){
             		label(
                    	title		: "Name the zone"
                        ,required	: true
                    )
                    /*
					only stock device types work in the list below???
                    ticket submitted, as this should work, and seems to work for everyone except me...
					*/
                   input(
                        name			: "vents"
                        ,title			: "Keen vents in this Zone:"
                        ,multiple		: true
                        ,required		: true
                        //,type			: "device.KeenHomeSmartVent"
                        ,type			: "capability.switchLevel"
                        ,submitOnChange	: soc
					)
 				 	input(
            			name		: "tempSensors"
                		,title		: "Temp Sensors:"
                		,multiple	: false
                		,required	: true
                		,type		: "capability.temperatureMeasurement"
                        ,submitOnChange	: soc
            		) 
                    /* out for now...
					input(
            			name		: "motionSensors"
                		,title		: "Motion Sensors:"
                		,multiple	: true
                		,required	: false
                		,type		: "capability.motionSensor"
            		)   
                    */
            }
            section("Zone Settings"){
					input(
            			name			: "minVo"
                		,title			: "Minimum vent opening"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options		: minVoptions()
                        ,defaultValue	: ["20"]
                        ,submitOnChange	: soc
            		) 
					input(
            			name			: "maxVo"
                		,title			: "Maximum vent opening"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options		: [["35":"35%"],["40":"40%"],["45":"45%"],["50":"50%"],["55":"55%"],["60":"60%"],["65":"65%"],["70":"70%"],["80":"80%"],["100":"Fully open"]]
                        ,defaultValue	: ["100"]
                        ,submitOnChange	: soc
            		) 
					input(
            			name			: "heatOffset"
                		,title			: "Heating offset, (above or below main thermostat)"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options 		: zoneTempOptions()
                        ,defaultValue	: ["0"]
                        ,submitOnChange	: soc
            		) 
					input(
            			name			: "coolOffset"
                		,title			: "Cooling offset, (above or below main thermostat)"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                        ,options 		: zoneTempOptions()
                        ,defaultValue	: ["0"]
                        ,submitOnChange	: soc
            		)    
                    input(
            			name			: "zoneControlSwitch"
                		,title			: "Optional zone disable switch\n(on=enable management)\n(off=disable management)"
                		,multiple		: false
                		,required		: false
                		,type			: "capability.switch"
                        ,submitOnChange	: true
            		)  
                    if (zoneControlSwitch){
                    	def zcsTitle = zoneInactiveOptions ? "When zone changes to off, set vent opening to:" : "When zone changes to off, vent opening will not be changed"
                    	input(
            				name			: "zoneInactiveOptions"
                			,title			: zcsTitle
                			,multiple		: false
                			,required		: false
                			,type			: "enum"
                        	,options		: minVoptions()
                        	,submitOnChange	: true
                            //,defaultValue	: "${minVo}"
            			)
                    }
            }
            //clean this up later...
            if (zoneControlSwitch) state.zoneControlActive = zoneControlSwitch.currentValue("switch") == "on"
    		else state.zoneControlActive = true
            //no point in harassing the mobile app or the vents unless we need to
            if (soc == true) systemOn(state.mainSetPoint,state.hvacMode)
	}
}
def minVoptions(){
	return [["0":"Fully closed"],["5":"5%"],["10":"10%"],["15":"15%"],["20":"20%"],["25":"25%"],["30":"30%"]]
}

def zoneTempOptions(){
	def zo
    if (!state.tempScale) state.tempScale = location.temperatureScale
	if (state.tempScale == "F"){
    	zo = [["-5":"-5°F"],["-4":"-4°F"],["-3":"-3°F"],["-2":"-2°F"],["-1":"-1°F"],["0":"0°F"],["1":"1°F"],["2":"2°F"],["3":"3°F"],["4":"4°F"],["5":"5°F"]]
    } else {
    	zo = [["-5":"-5°C"],["-4":"-4°C"],["-3":"-3°C"],["-2":"-2°C"],["-1":"-1°C"],["0":"0°C"],["1":"1°C"],["2":"2°C"],["3":"3°C"],["4":"4°C"],["5":"5°C"]]
    }
	return zo
}

//report methods, called from parent
def getEndReport(){
	return state.endReport ?: "\n\tNo data available yet."
}

def getZoneConfig(){
	//zoneControlSwitch
    def zc = "Not Activated" 
    if (zoneControlSwitch) zc = "is ${zoneControlSwitch.currentValue("switch")} via [${zoneControlSwitch.displayName}]"
	return "\n\tKeen vents: ${vents}\n\tTemp Sensors: [${tempSensors}]\n\tMinimum vent opening: ${minVo}%\n\tMaximum vent opening: ${maxVo}%\n\tHeating offset: ${tempStr(heatOffset)}\n\tCooling offset: ${tempStr(coolOffset)}\n\tZone control: ${zc}"
}

def getZoneState(){
    def s 
    if (state.running == true) s = true
    else s = false
    return "\n\trunning: ${s}\n\tset point: ${tempStr(state.setPoint)}\n\tcurrent temp: ${tempStr(tempSensors.currentValue("temperature"))}\n\tvent levels: ${vents.currentValue("level")}%"
}

def getVentReport(){
	def report = []
    vents.each{ vent ->
    	def P = vent.currentState("pressure")
        def L = vent.currentState("level")
        def T = vent.currentState("temperature")
        def set = [P:[D:P.date.format("yyyy-MM-dd HH:mm:ss") ,V:P.value],L:[D:L.date.format("yyyy-MM-dd HH:mm:ss") ,V:L.value],T:[D:T.date.format("yyyy-MM-dd HH:mm:ss") ,V:T.value]]
        //log.debug "vent:${vent}"
        //vent.properties.each{ p ->
        //	log.info "property:${p}"
        //}
        //def set = [T:[D:T.date ,V:T.value]]
        report.add((vent.displayName):set)
    }
    return report.toString() ?: "nothing new..."
}

//zone control methods
def systemOn(setPoint,hvacMode){
	def cTemp = tempSensors.currentValue("temperature")
    state.hvacMode = hvacMode
    state.mainSetPoint = setPoint
    
    if (zoneControlSwitch) state.zoneControlActive = zoneControlSwitch.currentValue("switch") == "on"
    else state.zoneControlActive = true
    
    def changeRequired = updateZoneSetpoint(setPoint,hvacMode)
    
    log.info "zc states: ${state.zoneControlActive} spChange required?${changeRequired}"
    
   	//change to a function, return setpoint change true/false
    //use to limit executions when not required
   
    
    if (state.zoneControlActive) {
    	//snapshot temp and pressure on zone start up
    	if (hvacMode != "idle"){
    		vents.each{ vent ->
    			def ct = vent.currentValue("temperature").toFloat()
				state."${vent.id}" = [P:vent.currentValue("pressure"),T:tempToK(ct)] 	
    		}
		} 
    	if (hvacMode == "heating"){
    		if (cTemp < state.setPoint){
    			state.running = true
    			vents.setLevel(maxVo.toInteger())
    			log.info "System heat on, vents set to:${maxVo.toInteger()}%"
    		} else {
    			state.running = false
            	vents.setLevel(minVo.toInteger())
    			log.info "System on, nothing to do, heating set point already met"
    		}         
    	} else if (hvacMode == "cooling"){
    		if (cTemp >= state.setPoint){
    			state.running = true
    			vents.setLevel(maxVo.toInteger())
    			log.info "System cool on, vents set to:${maxVo.toInteger()}%"
    		} else {
    			state.running = false
            	vents.setLevel(minVo.toInteger())
    			log.info "System on, nothing to do, cooling set point already met"
    		}         
    	} else {
    		//something pithy here...tempStr(cTemp)
    	}
    	log.info "systemOn- mode: ${hvacMode}, main setPoint: ${tempStr(setPoint)}, zone setPoint: ${tempStr(state.setPoint)}, current zone temp: ${tempStr(cTemp)}, vent levels: ${vents.currentValue("level")}%"
    } else {
    	state.running = false
    	log.info "systemOn- mode: ${hvacMode}, zone deactived via zone control switch"
    }
}

def systemOff(){
	state.running = false
	if (state.zoneControlActive) {
 		def ct = tempSensors.currentValue("temperature")
    	def zsp = state.setPoint
    	def d = ct- zsp
		log.info "systemOff- vent levels: ${vents.currentValue("level")}%, zone setPoint: ${tempStr(zsp)}, zone temp: ${tempStr(ct)}, variance: ${tempStr(d)}"
    	state.endReport = "\n\tset point: ${tempStr(zsp)}\n\tend temp: ${tempStr(ct)}\n\tvariance: ${tempStr(d)}\n\tvent levels: ${vents.currentValue("level")}%"
    	state.hvacMode = "idle"
    } else {
    	//log.info "systemOff- , zone is via zone control switch"
        if (zoneInactiveOptions) {
        	log.info "systemOff- , zone is deactived via zone control switch, setting vents to:${zoneInactiveOptions}"
            vents.setLevel(zoneInactiveOptions.toInteger())
        } else {
        	log.info "systemOff- , zone is deactived via zone control switch"
        }
    }
    
}

def updateZoneSetpoint(setPoint,hvacMode){
	//log.debug "updateZoneSetpoint called: main setpoint:${setPoint}"
    //heat sp change required?:false, newSP:75, oldSP:73
    def changeRequired = false
	if (hvacMode == "heating"){
    	def hsp = setPoint + heatOffset.toInteger()
        //log.debug "heat set point change required?:${state.setPoint != hsp}, newSP:${hsp}, oldSP:${state.setPoint}"
        if (state.setPoint != hsp){
        	//set point adjusted
            log.debug "heat set point change required: newSP:${hsp}, oldSP:${state.setPoint}"
        	state.setPoint = hsp
            changeRequired = true
        }
    } else if (hvacMode == "cooling"){
    	def csp = setPoint + coolOffset.toInteger()
        //log.debug "cool set point change required?:${state.setPoint != csp}, newSP:${csp}, oldSP:${state.setPoint}"
        if (state.setPoint != csp){
        	//set point adjusted
            log.debug "cool set point change required: newSP:${csp}, oldSP:${state.setPoint}"
        	state.setPoint = csp
            changeRequired = true
        }
        
   }
   return changeRequired
}

def ventHandler(evt){
	//def vo = tempStr(evt.floatValue)
	log.info "ventHandler- current zone temp:${tempStr(evt.floatValue)}, running:${state.running}, zone setPoint:${tempStr(state.setPoint)}"
    //def T1 = state.T1
    //def P1 = state.P1
    //def T2 = evt.floatValue
    //if (T1 && T2 && P1) log.info "adjusted pressure:${(P1 * T2)/T1}"

	if (state.running && state.setPoint){
    	if (evt.floatValue >= state.setPoint){
        	vents.setLevel(minVo.toInteger())
            state.running = false
            log.info "zone set point reached, setting vents to:${minVo.toInteger()}%"
        }
    }
}

def getAdjustedPressure(evt){
	if (state.running){
    	def vid = evt.deviceId
        def vent = vents.find{it.id == vid}
        
    	//find start up settings
    	def s = state."${vid}"
        //log.debug "vent:${evt.displayName}, start up state:${s}"
        if (s){
        	def P1 = s.P
            def T1 = s.T
             
            //T1 and T2 are integers P1 is a float 
        	//log.debug "initial- P1:${P1}, T1:${T1} event- name:${evt.name} value:${evt.value}"
            //pressure should be vs what it is...
            if (evt.name == "pressure"){
            	def TM = evt.value
            	def T2 = tempToK(vent.currentValue("temperature").toFloat())
                //def TC = (P1 * T2)/T1
                //number.round(0)	toDouble first???
                //log.info "pressure adjusted:${(P1 * T2)/T1}, reported:${evt.value}"
                //log.info "pressure adjusted:${TC}, reported:${TM}"
                log.debug "raw: P1:${P1}, T1:${T1}, T2:${T2}, TM:${TM}"
                
                //log.debug "formatted: P1:${P1}, T1:${T1}, T2:${T2}, TM:${TM}"
            }
        }
    }
}

//misc utility methods
def tempStr(temp){
    def tc = state.tempScale ?: location.temperatureScale
    if (temp) return "${temp.toString()}°${tc}"
    else return "No data available yet."
}

def tempToK(ct){
   	def K
   	if (state.tempScale == "F"){
		//F to K: [K] = ([°F] + 459.67) × 5⁄9
        K = ((ct + 459.67) * 5) / 9
    } else {
    	//C to K: [K] = [°C] + 273.15
        K = ct + 273.15
    }
	return K.toInteger()        
}

//legacy data logging and statistics
//spent too much time on this to delete it yet.
def statHandler(evt){
	if (state.runMaps.size() < 10) {
		log.info "event:${evt.value}"
    	def key = evt.date.format("yyyy-MM-dd HH:mm:ss")
    	def v  = evt.value
    	def evtTime = evt.date.getTime()
    	if (v == "heating"){
    		//start
        	state.lastCalibrationStart = key
        	state.startTime = evtTime
        	state.startTemp = tempSensors.currentValue("temperature")
        	log.info "start -time:${state.startTime} -temp:${state.startTemp}"
    	} else if (v == "idle" && state.startTime) {
    		//end
        	state.endTime = evtTime
        	state.endTemp = tempSensors.currentValue("temperature")
        	log.info "end -time:${state.endTime} -temp:${state.endTemp}"
        
        	if (state.endTime > state.startTime && state.endTemp > state.startTemp ){
        		def BigDecimal dTemp  = (state.endTemp - state.startTemp)
            	def BigDecimal dTime = (state.endTime - state.startTime) / 3600000
            	def BigDecimal dph = dTemp / dTime
        		def value = ["dph":"${dph}" ,"dTime":"${dTime}" ,"dTemp":"${dTemp}", "vo":"${vents.currentValue("level")}"]
        		log.info "${value}"
            	if (state.runMaps.size == 0){
            		state.runMaps = ["${key}":"${value}"]
            	} else {
            		state.runMaps << ["${key}":"${value}"]
            	}
            	state.endTime = ""
            	state.startTime = ""
            	state.endTemp = ""
            	state.startTemp = ""
        	}
        }
    }
}