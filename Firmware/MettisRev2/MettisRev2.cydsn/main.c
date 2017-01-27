/*******************************************************************************
* File Name: main.c
*
* Version: 1.0
*
* Description:
*  Simple BLE example project that demonstrates how to configure and use
*  Cypress's BLE component APIs and application layer callback. Device
*  Information service is used as an example to demonstrate configuring
*  BLE service characteristics in the BLE component.
*
* References:
*  BLUETOOTH SPECIFICATION Version 4.1
*
* Hardware Dependency:
*  CY8CKIT-042 BLE
*
********************************************************************************
* Copyright 2015, Cypress Semiconductor Corporation. All rights reserved.
* This software is owned by Cypress Semiconductor Corporation and is protected
* by and subject to worldwide patent and copyright laws and treaties.
* Therefore, you may use this software only as provided in the license agreement
* accompanying the software package from which you obtained this software.
* CYPRESS AND ITS SUPPLIERS MAKE NO WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* WITH REGARD TO THIS SOFTWARE, INCLUDING, BUT NOT LIMITED TO, NONINFRINGEMENT,
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
*******************************************************************************/

#include "main.h"
#include "flex_sensors.h"
#include "BLEApplications.h"
#include "HandleLowPower.h"
#include "mettis.h"
#include "WriteUserSFlash.h"
#include "record.h"

/* This flag is used by application to know whether a Central 
* device has been connected. This is updated in BLE event callback 
* function*/
extern uint8 deviceConnected;

/*This flag is set when the Central device writes to CCCD of the 
* Raw Sensor Data Characteristic to enable notifications */
extern uint8 sendRawSensorDataNotification;

/*This flag is set when the Central device writes to CCCD of the 
* Playback Data Characteristic to enable notifications */
extern uint8 sendPlaybackDataNotification;

/* 'restartAdvertisement' flag is used to restart advertisement */
extern uint8 restartAdvertisement;

//State
#define UNCONNECTED 0
#define CONNECTING 1
#define CONNECTED 2

static int state = UNCONNECTED;

extern int orientation;

uint8 bootloadingMode = 0;

static int sensor_values[FLEX_SENSORS_NUM];
static int sensor_peaks[FLEX_SENSORS_NUM];
static uint8 sensor_scaled[FLEX_SENSORS_NUM];
static uint8 record_data[RECORD_DATA_SIZE];

static int advertise_counter = 0;

#define ADVERTISE_COUNT 100  //1 seconds

static int32 steps;

uint8 *adPacket;
uint8 adPacketLen;
uint8 manuDataOffset;
static uint8 manuDataType = 0;

/******************************************************************************/
int main()
{
	
	    
	InitializeSystem();
	
	flex_sensors_init();
		
	//flex_sensors_test();  //Gets orientation (left or right)
	
	//record_erase();
	
	
	DBG_PRINT_TEXT("Mettis trainer up\r\n");

    while(1u == 1u)
    {
        
		
        CyBle_ProcessEvents();
		
		
		//Currently called every 1 ms
		mettis_task(sensor_peaks, sensor_values);	
		
		if (record_isRecording() ) {
		
			record_data[0] = (sensor_values[0]>>4) & 0xFF;
			record_data[1] = (sensor_values[0]>>12) & 0xFF;
			record_data[2] = (sensor_values[1]>>4) & 0xFF;
			record_data[3] = (sensor_values[1]>>12) & 0xFF;
			record_data[4] = (sensor_values[2]>>4) & 0xFF;
			record_data[5] = (sensor_values[2]>>12) & 0xFF;
			
			record_post_data(record_data);
		}
		
		record_task();
		
		
		
		
		
		
		
		
		
				
		switch (state) {
			case CONNECTING:
			
				//Perform just connected initialization here
				
			
			
			
				state = CONNECTED;
			
			break;
		
			case CONNECTED:
			
				if(TRUE != deviceConnected) {
					state = UNCONNECTED;
					break;
				}
			
				if(sendRawSensorDataNotification & CCCD_NTF_BIT_MASK)
				{
					
					//Execute sensor measurements
					
				}
			
			break;
		
			default: //Unconnected 
				
				if (advertise_counter == 0) {
					advertise_counter = ADVERTISE_COUNT;
					
					restartAdvertisement = TRUE;
					
					//DBG_PRINT_TEXT("Advertise restarted\r\n");
					
					
					
					
					//Update local name with latest step data
					//steps = cadence_get_total_steps();
					steps++;
										
					adPacket = cyBle_discoveryData.advData;
                	adPacketLen = cyBle_discoveryData.advDataLen;
					//Last byte of adPacket is the length
					//8 bytes before the length are the manufacture specific data
					manuDataOffset = adPacketLen - 8;
					
					if (manuDataType == 0) {
						//Step data
						adPacket[manuDataOffset] = 0x00; //Step data
						adPacket[manuDataOffset+1] = 0x00;
						adPacket[manuDataOffset+2] = 0x00;
						adPacket[manuDataOffset+3] = 0x00;
						adPacket[manuDataOffset+4] = (steps>>24) & 0xFF;
						adPacket[manuDataOffset+5] = (steps>>16) & 0xFF;
						adPacket[manuDataOffset+6] = (steps>>8) & 0xFF;
						adPacket[manuDataOffset+7] = (steps>>0) & 0xFF;
					} else {
						adPacket[manuDataOffset] = 0x01; //Other metric data
						adPacket[manuDataOffset+1] = cadence_get_cadence();
						adPacket[manuDataOffset+2] = cadence_get_pronation();
						adPacket[manuDataOffset+3] = cadence_get_impact_force();
						adPacket[manuDataOffset+4] = cadence_get_heel2toe();
						adPacket[manuDataOffset+5] = cadence_get_contact_time();
						adPacket[manuDataOffset+6] = cadence_get_air_time();
						adPacket[manuDataOffset+7] = 0x00;
					}
					
					manuDataType ^= 1;
					
					CYBLE_GAP_BD_ADDR_T bdAddr;	
					CyBle_GetDeviceAddress(&bdAddr);
					if ( (bdAddr.bdAddr[0] & 0x01) == 0u) {
						orientation = FLEX_SENSORS_TEST_RESULT_LEFT;
					} else {
						orientation = FLEX_SENSORS_TEST_RESULT_RIGHT;
					}
				
				
				} else {
					advertise_counter--;
				}
				
				
				
				
				
				
				
				
				
				
				
				if(TRUE == deviceConnected) {
					state = CONNECTING;
				}
			break;
		
		}
		
		
		if(TRUE == deviceConnected)
		{
			/* After the connection, send new connection parameter to the Client device 
			* to run the BLE communication on desired interval. This affects the data rate 
			* and power consumption. High connection interval will have lower data rate but 
			* lower power consumption. Low connection interval will have higher data rate at
			* expense of higher power. This function is called only once per connection. */
			UpdateConnectionParam();
			
			/* When the Client Characteristic Configuration descriptor (CCCD) is written
			* by Central device for enabling/disabling notifications, then the same
			* descriptor value has to be explicitly updated in application so that
			* it reflects the correct value when the descriptor is read */
			UpdateNotificationCCCD();
			
			/* Send Raw Sensor data when respective notification is enabled */
			if(sendRawSensorDataNotification & CCCD_NTF_BIT_MASK)
			{
				HandleRawData();
			}
		
			/* Send playback data when respective notification is enabled */
			if(sendPlaybackDataNotification & CCCD_NTF_BIT_MASK)
			{
				HandlePlaybackData();
			}
		
		}
		
		
		/* Put system to Deep sleep, including BLESS, and wakeup on interrupt. 
		* The source of the interrupt can be either BLESS Link Layer in case of 
		* BLE advertisement and connection or by watch dog timer */
		HandleLowPowerMode();

		if(restartAdvertisement)
		{
			/* Reset 'restartAdvertisement' flag*/
			restartAdvertisement = FALSE;
			
			/* Start Advertisement and enter Discoverable mode*/
			CyBle_GappStartAdvertisement(CYBLE_ADVERTISING_FAST);	
			
		}
		
        if (bootloadingMode == 1u)
        {
            DBG_PRINTF("Bootloader service activated!\r\n");
            CyBle_GattsEnableAttribute(cyBle_customs[0u].customServiceHandle);
            //Original LED off
            bootloadingMode = 2u;

            /* Force client to rediscover services in range of bootloader service */
            WriteAttrServChanged();

            BootloaderEmulator_Start();
        }
		
    }
}

/*******************************************************************************
* Function Name: InitializeSystem
********************************************************************************
* Summary:
*        Start the components and initialize system 
*
* Parameters:
*  void
*
* Return:
*  void
*
*******************************************************************************/
void InitializeSystem(void)
{
	const char8 serialNumber[] = SERIAL_NUMBER;
    

    DBG_PRINT_TEXT("\r\n");
    DBG_PRINT_TEXT("\r\n");
    DBG_PRINT_TEXT("===============================================================================\r\n");
    DBG_PRINT_TEXT("=              Mettis Trainer Application Started            \r\n");
    DBG_PRINT_TEXT("=              Version: 0.01.a                                                   \r\n");
    DBG_PRINTF    ("=              Compile Date and Time : %s %s                                   \r\n", __DATE__,__TIME__);
    DBG_PRINT_TEXT("===============================================================================\r\n");
    DBG_PRINT_TEXT("\r\n"); 
    DBG_PRINT_TEXT("\r\n");   

    CyGlobalIntEnable;

    LED_Write(LED_OFF);

    
    CyBle_Start(AppCallBack);
    
    /*Initialization of encryption in BLE stack*/
    #if (ENCRYPTION_ENABLED == YES)
        CR_Initialization();
    #endif /*(ENCRYPTION_ENABLED == YES)*/
    
    
    /* Set Serial Number string not initialised in GUI */
    CyBle_DissSetCharacteristicValue(CYBLE_DIS_SERIAL_NUMBER, sizeof(serialNumber), (uint8 *)serialNumber);

    /* Disable bootloader service */
    CyBle_GattsDisableAttribute(cyBle_customs[0].customServiceHandle);

    /* Force client to rediscover services in range of bootloader service */
    WriteAttrServChanged();
    
    WDT_Start();
	
	EMI_SPIM_Start();
	
	

}

/*******************************************************************************
* Function Name: HandleRawData
********************************************************************************
* Summary:
*        This function scans for finger position on CapSense slider, and if the  
* position is different, triggers separate routine for BLE notification
*
* Parameters:
*  void
*
* Return:
*  void
*
*******************************************************************************/
void HandleRawData(void)
{
	
	
	uint8 rawSensorData[RAW_SENSOR_NTF_DATA_LEN];
	uint8 cadence;
	uint8 contact_time;
	uint8 heel2toe;

	int status;
	//uint8 sensor_state;
	
	if (record_isPlayback() ) {
		
		uint8 playbackData[PLAYBACK_NTF_DATA_LEN];
			
		record_play_next(playbackData);
		
		rawSensorData[0] = playbackData[1];
		rawSensorData[1] = playbackData[3];
		rawSensorData[2] = playbackData[5];
		rawSensorData[3] = playbackData[0];
		rawSensorData[4] = playbackData[2];
		rawSensorData[5] = playbackData[4];
		
		//DBG_PRINT_ARRAY(playbackData, RECORD_DATA_SIZE); DBG_PRINTF("\r\n");	
		
	} else {
			
		//Execute sensor measurements
		cadence_convert_scaled(sensor_scaled, sensor_peaks);  //Use peak values between BLE communication
						
		cadence = cadence_get_cadence();
		contact_time = cadence_get_contact_time();
		heel2toe = cadence_get_heel2toe();
		
		//sensor_state = mettis_get_state();
		
		rawSensorData[0] = sensor_scaled[0];
		rawSensorData[1] = sensor_scaled[1];
		rawSensorData[2] = sensor_scaled[2];
		rawSensorData[3] = cadence;
		rawSensorData[4] = contact_time;
		rawSensorData[5] = heel2toe;
	}

	
	status = SendDataOverRawSensorNotification(rawSensorData);
	if (status) {
		flex_sensors_reset_peaks(sensor_peaks);  //Reset peaks
	}
}

/*******************************************************************************
* Function Name: HandlePlaybackData
********************************************************************************
* Summary:
*        This function scans for finger position on CapSense slider, and if the  
* position is different, triggers separate routine for BLE notification
*
* Parameters:
*  void
*
* Return:
*  void
*
*******************************************************************************/
void HandlePlaybackData(void)
{
	uint8 playbackData[PLAYBACK_NTF_DATA_LEN];
			
	record_play_next(playbackData);
		
	//SendDataOverRecordPlaybackNotification(playbackData);

}







/* [] END OF FILE */

