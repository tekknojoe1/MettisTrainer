/******************************************************************************
* Project Name		: PSoC_4_BLE_CapSense_Slider_LED
* File Name			: BLEApplications.c
* Version 			: 1.0
* Device Used		: CY8C4247LQI-BL483
* Software Used		: PSoC Creator 3.1
* Compiler    		: ARM GCC 4.8.4, ARM RVDS Generic, ARM MDK Generic
* Related Hardware	: CY8CKIT-042-BLE Bluetooth Low Energy Pioneer Kit 
* Owner             : ROIT
*
********************************************************************************
* Copyright (2014), Cypress Semiconductor Corporation. All Rights Reserved.
********************************************************************************
* This software is owned by Cypress Semiconductor Corporation (Cypress)
* and is protected by and subject to worldwide patent protection (United
* States and foreign), United States copyright laws and international treaty
* provisions. Cypress hereby grants to licensee a personal, non-exclusive,
* non-transferable license to copy, use, modify, create derivative works of,
* and compile the Cypress Source Code and derivative works for the sole
* purpose of creating custom software in support of licensee product to be
* used only in conjunction with a Cypress integrated circuit as specified in
* the applicable agreement. Any reproduction, modification, translation,
* compilation, or representation of this software except as specified above 
* is prohibited without the express written permission of Cypress.
*
* Disclaimer: CYPRESS MAKES NO WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, WITH 
* REGARD TO THIS MATERIAL, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
* Cypress reserves the right to make changes without further notice to the 
* materials described herein. Cypress does not assume any liability arising out 
* of the application or use of any product or circuit described herein. Cypress 
* does not authorize its products for use as critical components in life-support 
* systems where a malfunction or failure may reasonably be expected to result in 
* significant injury to the user. The inclusion of Cypress' product in a life-
* support systems application implies that the manufacturer assumes all risk of 
* such use and in doing so indemnifies Cypress against all charges. 
*
* Use of this Software may be limited by and subject to the applicable Cypress
* software license agreement. 
*******************************************************************************/
#include "main.h"
#include "BLEApplications.h"
#include "flex_sensors.h"
#include "record.h"
#include "mettis.h"

/**************************Variable Declarations*****************************/


/*This flag is set when the Central device writes to CCCD (Client Characteristic 
* Configuration Descriptor) of the Raw Sensor Data Characteristic to enable 
* notifications */
uint8 sendRawSensorDataNotification = FALSE;

/*This flag is set when the Central device writes to CCCD (Client Characteristic 
* Configuration Descriptor) of the Record Playback Data Characteristic to enable 
* notifications */
uint8 sendPlaybackDataNotification = FALSE;


/* This flag is used by application to know whether a Central 
* device has been connected. This is updated in BLE event callback 
* function*/
uint8 deviceConnected = FALSE;

/* 'restartAdvertisement' flag provided the present state of power mode in firmware */
uint8 restartAdvertisement = FALSE;

/* These flags are used to let application update the respective CCCD value of the 
* custom characteristics for correct read operation by connected Central device */
uint8 updateRawSensorDataNotificationCCCDAttribute = FALSE;

/* These flags are used to let application update the respective CCCD value of the 
* custom characteristics for correct read operation by connected Central device */
uint8 updatePlaybackDataNotificationCCCDAttribute = FALSE;

/* This flag is used to let application send a L2CAP connection update request
* to Central device */
static uint8 isConnectionUpdateRequested = TRUE;

/* Counter to keep the LED ON for a selected period before shuting the LEDs down */
uint8 led_timer = FALSE;

/* Counter to allow an initial 3 second Status LED ON for indicating connection */
uint8 timer_tick = FALSE;


/* Status flag for the Stack Busy state. This flag is used to notify the application 
* whether there is stack buffer free to push more data or not */
uint8 busyStatus = 0;


CYBLE_CONN_HANDLE_T connHandle;

/* Connection Parameter update values. This values are used by the BLE component
* to update the connector parameter, including connection interval, to desired 
* value */
static CYBLE_GAP_CONN_UPDATE_PARAM_T ConnectionParam =
{
    CONN_PARAM_UPDATE_MIN_CONN_INTERVAL,  		      
    CONN_PARAM_UPDATE_MAX_CONN_INTERVAL,		       
    CONN_PARAM_UPDATE_SLAVE_LATENCY,			    
    CONN_PARAM_UPDATE_SUPRV_TIMEOUT 			         	
};

/* Buffer for received data */
uint8 packetRX[BLE_PACKET_SIZE_MAX];
uint32 packetRXSize;
uint32 packetRXFlag;

uint8 packetTX[BLE_PACKET_SIZE_MAX];
uint32 packetTXSize;

extern uint8 bootloadingMode;


/****************************************************************************/

/*******************************************************************************
* Function Name: AppCallBack()
********************************************************************************
*
* Summary:
*   This finction handles events that are generated by BLE stack.
*
* Parameters:
*   None
*
* Return:
*   None
*
*******************************************************************************/
void AppCallBack(uint32 event, void* eventParam)
{
    CYBLE_API_RESULT_T apiResult;
    uint32  i = 0u;
	//CYBLE_GAP_BD_ADDR_T bdAddr;	
	
	/* Local variable to store the data received as part of the Write request 
	* events */
	CYBLE_GATTS_WRITE_REQ_PARAM_T *wrReqParam;
	
	//DBG_PRINT_TEXT("Callback called\r\n");
   
    switch (event)
    {
        /**********************************************************
        *                       General Events
        ***********************************************************/
        case CYBLE_EVT_STACK_ON: /* This event received when component is Started */
            /* Enter in to discoverable mode so that remote can search it. */
            apiResult = CyBle_GappStartAdvertisement(CYBLE_ADVERTISING_FAST);
            if(apiResult != CYBLE_ERROR_OK)
            {
				
										
				
            }
            break;
        case CYBLE_EVT_HARDWARE_ERROR:    /* This event indicates that some internal HW error has occurred. */
            DBG_PRINTF("CYBLE_EVT_HARDWARE_ERROR\r\n");
            break;
            

        /**********************************************************
        *                       GAP Events
        ***********************************************************/
        case CYBLE_EVT_GAP_AUTH_REQ:
            DBG_PRINTF("EVT_AUTH_REQ: security=%x, bonding=%x, ekeySize=%x, err=%x \r\n",
                (*(CYBLE_GAP_AUTH_INFO_T *)eventParam).security,
                (*(CYBLE_GAP_AUTH_INFO_T *)eventParam).bonding,
                (*(CYBLE_GAP_AUTH_INFO_T *)eventParam).ekeySize,
                (*(CYBLE_GAP_AUTH_INFO_T *)eventParam).authErr);
            break;
        case CYBLE_EVT_GAP_PASSKEY_ENTRY_REQUEST:
            DBG_PRINTF("EVT_PASSKEY_ENTRY_REQUEST press 'p' to enter passkey \r\n");
            break;
        case CYBLE_EVT_GAP_PASSKEY_DISPLAY_REQUEST:
            DBG_PRINTF("EVT_PASSKEY_DISPLAY_REQUEST %6.6ld \r\n", *(uint32 *)eventParam);
            break;
        case CYBLE_EVT_GAP_KEYINFO_EXCHNGE_CMPLT:
            DBG_PRINTF("EVT_GAP_KEYINFO_EXCHNGE_CMPLT \r\n");
            break;
        case CYBLE_EVT_GAP_AUTH_COMPLETE:
            DBG_PRINTF("AUTH_COMPLETE");
            break;
        case CYBLE_EVT_GAP_AUTH_FAILED:
            DBG_PRINTF("EVT_AUTH_FAILED: %x \r\n", *(uint8 *)eventParam);
            break;
        case CYBLE_EVT_GAP_DEVICE_CONNECTED:
            DBG_PRINTF("EVT_GAP_DEVICE_CONNECTED: %d \r\n", connHandle.bdHandle);
            //LED change device is connected
            break;
        case CYBLE_EVT_GAP_DEVICE_DISCONNECTED:
            DBG_PRINTF("EVT_GAP_DEVICE_DISCONNECTED\r\n");
            apiResult = CyBle_GappStartAdvertisement(CYBLE_ADVERTISING_FAST);
            if(apiResult != CYBLE_ERROR_OK)
            {
                DBG_PRINTF("StartAdvertisement API Error: %d \r\n", apiResult);
            }
            break;
        case CYBLE_EVT_GAP_ENCRYPT_CHANGE:
            DBG_PRINTF("EVT_GAP_ENCRYPT_CHANGE: %x \r\n", *(uint8 *)eventParam);
            break;
        case CYBLE_EVT_GAPC_CONNECTION_UPDATE_COMPLETE:
            DBG_PRINTF("EVT_CONNECTION_UPDATE_COMPLETE: %x \r\n", *(uint8 *)eventParam);
            break;
        case CYBLE_EVT_GAPP_ADVERTISEMENT_START_STOP:
			
/*
			//Update MAC address if left or right sensor is different
			CyBle_GetDeviceAddress(&bdAddr);
			
			if ( (bdAddr.bdAddr[0] & 0x01) == 0u) {
				//Even mac address
				if (flex_sensors_orientation() == FLEX_SENSORS_TEST_RESULT_RIGHT ) {
					
					bdAddr.bdAddr[0] |= 0x01; //Make odd
					
					//Update flash location
					//(void) SF_WriteUserSFlashRow(USER_MAC_ADDRESS_FLASH_ADDRESS, );
					
					(void) CyBle_SetDeviceAddress(&bdAddr);
		            #if(CYBLE_GAP_ROLE_PERIPHERAL || CYBLE_GAP_ROLE_BROADCASTER)
		                CyBle_ChangeAdDeviceAddress(&bdAddr, 0u);
		                CyBle_ChangeAdDeviceAddress(&bdAddr, 1u);
		            #endif // CYBLE_GAP_ROLE_PERIPHERAL || CYBLE_GAP_ROLE_BROADCASTER     
					
				}
				
			} else {
				//Odd mac address
				
				if (flex_sensors_orientation() == FLEX_SENSORS_TEST_RESULT_LEFT ) {
					
					bdAddr.bdAddr[0] &= ~0x01; //Make even
					
					(void) CyBle_SetDeviceAddress(&bdAddr);
		            #if(CYBLE_GAP_ROLE_PERIPHERAL || CYBLE_GAP_ROLE_BROADCASTER)
		                CyBle_ChangeAdDeviceAddress(&bdAddr, 0u);
		                CyBle_ChangeAdDeviceAddress(&bdAddr, 1u);
		            #endif // CYBLE_GAP_ROLE_PERIPHERAL || CYBLE_GAP_ROLE_BROADCASTER  
					
				}
				
			}		
*/	
			
            if(CYBLE_STATE_DISCONNECTED == CyBle_GetState())
            {   
				/* Fast and slow advertising period complete */ 
				DBG_PRINT_TEXT("Advertising period complete\r\n");
				
				if (bootloadingMode != 0u) {
					//Restart advertising when in bootloader mode always
					CyBle_GappStartAdvertisement(CYBLE_ADVERTISING_FAST);	
				}
				
            }
            break;

            
        /**********************************************************
        *                       GATT Events
        ***********************************************************/
        case CYBLE_EVT_GATTS_WRITE_REQ:
            /* This event is received when Central device sends a Write command on an Attribute */
            wrReqParam = (CYBLE_GATTS_WRITE_REQ_PARAM_T *) eventParam;
			
			 /* Pass packet to bootloader emulator */
            packetRXSize = ((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair.value.len;
            memcpy(&packetRX[0], ((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair.value.val, packetRXSize);

			DBG_PRINTF("EVT_GATT_WRITE_REQ: %x = ",((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair.attrHandle);
            for(i = 0; i < ((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair.value.len; i++)
            {
                DBG_PRINTF("%2.2x ", ((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair.value.val[i]);
            }
            DBG_PRINTF("\r\n");
            CyBle_GattsWriteAttributeValue(&((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair, 0u, \
                        &((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->connHandle, CYBLE_GATT_DB_PEER_INITIATED);

			
			ProcessCMD(wrReqParam, packetRX, packetRXSize);
			
			/* Send the response to the write request received. */
            (void)CyBle_GattsWriteRsp(((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->connHandle);

			
            break;
        case CYBLE_EVT_GATT_CONNECT_IND:
            connHandle = *(CYBLE_CONN_HANDLE_T *)eventParam;
			
			packetRXFlag = 0u;
			
			/* This flag is used in application to check connection status */
			deviceConnected = TRUE;
            break;
        case CYBLE_EVT_GATT_DISCONNECT_IND:
            connHandle.bdHandle = 0;
			
			/* Update deviceConnected flag*/
			deviceConnected = FALSE;
			
			/* Reset Raw Sensor Data notification flag to prevent further notifications
			 * being sent to Central device after next connection. */
			sendRawSensorDataNotification = FALSE;
			
			/* Reset Playback Data notification flag to prevent further notifications
			 * being sent to Central device after next connection. */
			sendPlaybackDataNotification = FALSE;
			
			
			/* Reset the CCCD value to disable notifications */
			updateRawSensorDataNotificationCCCDAttribute = TRUE;
			updatePlaybackDataNotificationCCCDAttribute = TRUE;
			UpdateNotificationCCCD();
			
			

			/* Reset the isConnectionUpdateRequested flag to allow sending
			* connection parameter update request in next connection */
			isConnectionUpdateRequested = TRUE;
            break;
        case CYBLE_EVT_GATTS_WRITE_CMD_REQ:
			
			wrReqParam = (CYBLE_GATTS_WRITE_REQ_PARAM_T *) eventParam;
			
            /* Pass packet to bootloader emulator */
            packetRXSize = ((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair.value.len;
            memcpy(&packetRX[0], ((CYBLE_GATTS_WRITE_REQ_PARAM_T *)eventParam)->handleValPair.value.val, packetRXSize);
            packetRXFlag = 1u;
			
			DBG_PRINTF("WRITE_CMD_REQ: %x %x\r\n", wrReqParam->handleValPair.attrHandle, wrReqParam->handleValPair.value.val[CYBLE_SHOE_SENSOR_DATA_RAW_DATA_FIELD_CLIENT_CHARACTERISTIC_CONFIGURATION_DESC_INDEX]);
			
			ProcessCMD(wrReqParam, packetRX, packetRXSize);
			
            break;
        case CYBLE_EVT_GATTS_PREP_WRITE_REQ:
            (void)CyBle_GattsPrepWriteReqSupport(CYBLE_GATTS_PREP_WRITE_NOT_SUPPORT);
			
			DBG_PRINT_TEXT("PREP_WRITE_REQ:\r\n");
            break;
        case CYBLE_EVT_HCI_STATUS:
            DBG_PRINTF("CYBLE_EVT_HCI_STATUS\r\n");
			break;
			
		case CYBLE_EVT_L2CAP_CONN_PARAM_UPDATE_RSP:
				/* If L2CAP connection parameter update response received, reset application flag */
            	isConnectionUpdateRequested = FALSE;
            break;
			
		case CYBLE_EVT_STACK_BUSY_STATUS:
			/* This event is generated when the internal stack buffer is full and no more
			* data can be accepted or the stack has buffer available and can accept data.
			* This event is used by application to prevent pushing lot of data to stack. */
			
			/* Extract the present stack status */
            busyStatus = * (uint8*)eventParam;
            break;	
			
        default:
			
			DBG_PRINT_TEXT("default:\r\n");
			
            break;
        }
}

/*******************************************************************************
* Function Name: ProcessCMD
********************************************************************************
* Summary:
*        Processes commands received by BLE
*
* Parameters:
*  packetRX - pointer to the packet	
*  packetRXSize - size of packet
*
* Return:
*  None
*
*******************************************************************************/
void ProcessCMD(CYBLE_GATTS_WRITE_REQ_PARAM_T *wrReqParam, uint8 packetRX[], uint32 packetRXSize ) {

	//Occurs then the shoe service is written to
	if (strncmp( (char*)packetRX, "BOOTLOAD", 8) == 0u ) {
		bootloadingMode = 1u; //Enable bootloading	
	} else if (strncmp( (char*)packetRX, "RESET", 5) == 0u ) {
		DBG_PRINT_ARRAY(packetRX, packetRXSize);
		DBG_PRINT_TEXT("Received BLE RESET\r\n");
		CySoftwareReset(); //Reset device
	} else if (strncmp( (char*)packetRX, "BASELINE", 8) == 0u ) {
		flex_sensors_force_baseline();
	} else if (strncmp( (char*)packetRX, "CALMAX", 6) == 0u ) {
		mettis_force_cal_max();	
		
		DBG_PRINT_TEXT("Received CALMAX\r\n");
		
	} else if (strncmp( (char*)packetRX, "CALMIN", 6) == 0u ) {
		mettis_force_cal_min();	
		
		DBG_PRINT_TEXT("Received CALMIN\r\n");
		
	} else if (strncmp( (char*)packetRX, "REC0", 4) == 0u ) {
		record_start(0);  //Start record mode 0		
	
	} else if (strncmp( (char*)packetRX, "PLAY", 4) == 0u ) {
		record_play();  //Playback data		
	
	} else if (strncmp( (char*)packetRX, "STOP", 4) == 0u ) {
		record_stop();  //Stop play or record		
	}
		
	/* When this event is triggered, the peripheral has received a write command on the custom characteristic */
	/* Check if command is for correct attribute and update the flag for sending Notifications */
    if(CYBLE_SHOE_SENSOR_DATA_RAW_DATA_FIELD_CLIENT_CHARACTERISTIC_CONFIGURATION_DESC_HANDLE == wrReqParam->handleValPair.attrHandle)
	{
		/* Extract the Write value sent by the Client for raw sensor data */
        sendRawSensorDataNotification = wrReqParam->handleValPair.value.val[CYBLE_SHOE_SENSOR_DATA_RAW_DATA_FIELD_CLIENT_CHARACTERISTIC_CONFIGURATION_DESC_INDEX];
		
		/* Set flag to allow CCCD to be updated for next read operation */
		updateRawSensorDataNotificationCCCDAttribute = TRUE;
		
		DBG_PRINT_TEXT("Enabled/Disabled sensor data feed\r\n");
    }
	
	
	/* When this event is triggered, the peripheral has received a write command on the custom characteristic */
	/* Check if command is for correct attribute and update the flag for sending Notifications */
    if(CYBLE_RECORD_PLAYBACK_PLAYBACK_DATA_CLIENT_CHARACTERISTIC_CONFIGURATION_DESC_HANDLE == wrReqParam->handleValPair.attrHandle)
	{
		/* Extract the Write value sent by the Client for raw sensor data */
        sendPlaybackDataNotification = wrReqParam->handleValPair.value.val[CYBLE_RECORD_PLAYBACK_PLAYBACK_DATA_CLIENT_CHARACTERISTIC_CONFIGURATION_DESC_INDEX];
		
		/* Set flag to allow CCCD to be updated for next read operation */
		updatePlaybackDataNotificationCCCDAttribute = TRUE;
		
		DBG_PRINT_TEXT("Enabled/Disabled playback data feed\r\n");
    }
	
}


/*******************************************************************************
* Function Name: SendDataOverRawSensorNotification
********************************************************************************
* Summary:
*        Send sensor data as BLE Notifications. This function updates
* the notification handle with data and triggers the BLE component to send 
* notification
*
* Parameters:
*  rawSensorData:	sensor data array	
*
* Return:
*  0 - stack was busy, 1 stack was free
*
*******************************************************************************/
int SendDataOverRawSensorNotification(uint8 rawSensorData[])
{
	CYBLE_GATTS_HANDLE_VALUE_NTF_T		rawSensorNotificationHandle;	
	
		
	/* If stack is not busy, then send the notification */
	if(busyStatus == CYBLE_STACK_STATE_FREE)
	{

		/* Update notification handle with sensor data*/
		rawSensorNotificationHandle.attrHandle = CYBLE_SHOE_SENSOR_DATA_RAW_DATA_FIELD_CHAR_HANDLE;
		rawSensorNotificationHandle.value.val = rawSensorData;
		rawSensorNotificationHandle.value.len = RAW_SENSOR_NTF_DATA_LEN;
		
		/* Send the updated handle as part of attribute for notifications */
		CyBle_GattsNotification(connHandle,&rawSensorNotificationHandle);
		
		return 1;
	}
	
	return 0;
}

/*******************************************************************************
* Function Name: SendDataOverRecordPlaybackNotification
********************************************************************************
* Summary:
*        Send sensor data as BLE Notifications. This function updates
* the notification handle with data and triggers the BLE component to send 
* notification
*
* Parameters:
*  rawSensorData:	sensor data array	
*
* Return:
*  0 - stack was busy, 1 stack was free
*
*******************************************************************************/
int SendDataOverRecordPlaybackNotification(uint8 playbackData[])
{
	CYBLE_GATTS_HANDLE_VALUE_NTF_T		playbackNotificationHandle;	
	
		
	/* If stack is not busy, then send the notification */
	if(busyStatus == CYBLE_STACK_STATE_FREE)
	{

		/* Update notification handle with sensor data*/
		playbackNotificationHandle.attrHandle = CYBLE_RECORD_PLAYBACK_PLAYBACK_DATA_CHAR_HANDLE;
		playbackNotificationHandle.value.val = playbackData;
		playbackNotificationHandle.value.len = PLAYBACK_NTF_DATA_LEN;
		
		/* Send the updated handle as part of attribute for notifications */
		CyBle_GattsNotification(connHandle,&playbackNotificationHandle);
		
		return 1;
	}
	
	return 0;
}

/*******************************************************************************
* Function Name: HandleLeds()
********************************************************************************
*
* Summary:
*   This function handles LEDs operation depending on the project operation
*   mode.
*
* Parameters:
*   None
*
* Return:
*   None
*
*******************************************************************************/
void HandleLeds(void)
{
    static uint32 ledTimer = LED_TIMEOUT;

	
	if(--ledTimer == 0u) {
    	ledTimer = LED_TIMEOUT;
	}
	
	if (bootloadingMode != 0u) {
		
		//DBG_PRINT_TEXT("LED state update mode...\r\n");
		
		//2 quick blinks then off
		LED_SetDriveMode(LED_DM_STRONG);
		if ( ( ( (ledTimer>>7) & 0xF) == 0) || ( ( (ledTimer>>7) & 0xF) == 2) ) {
			LED_Write(LED_ON);
		} else {
			LED_Write(LED_OFF);
		}
		
	} else if (CHGN_Read() == 0u) {
		
		//DBG_PRINT_TEXT("LED state charging...\r\n");
		
		//Fast quick blinks
		LED_SetDriveMode(LED_DM_STRONG);
		if ( ( (ledTimer>>6) & 0xF) == 0) {
			LED_Write(LED_ON);
		} else {
			LED_Write(LED_OFF);
		}
		
	} else if (CYBLE_STATE_ADVERTISING == CyBle_GetState() ) {
		//Advertising 
		
		//DBG_PRINT_TEXT("LED state advertising...\r\n");
		
		//1 quick blink
		LED_SetDriveMode(LED_DM_STRONG);
		if ( ( (ledTimer>>7) & 0xF) == 0) {
			LED_Write(LED_ON);
		} else {
			LED_Write(LED_OFF);
		}
		
	} else {
		LED_SetDriveMode(LED_DM_DIG_HIZ);
		LED_Write(LED_OFF);
	}
	
	
}

/*******************************************************************************
* Function Name: Timer_Interrupt
********************************************************************************
*
* Summary:
*  Handles the Interrupt Service Routine for the WDT timer.
*
*******************************************************************************/
CY_ISR(Timer_Interrupt)
{
    if(CySysWdtGetInterruptSource() & WDT_INTERRUPT_SOURCE)
    {
        HandleLeds();
        
        /* Clears interrupt request  */
        CySysWdtClearInterrupt(WDT_INTERRUPT_SOURCE);
    }
}

/*******************************************************************************
* Function Name: WDT_Start
********************************************************************************
*
* Summary:
*  Configures WDT to trigger an interrupt.
*
*******************************************************************************/

void WDT_Start(void)
{
    /* Unlock the WDT registers for modification */
    CySysWdtUnlock(); 
    /* Setup ISR */
    WDT_Interrupt_StartEx(&Timer_Interrupt);
    /* Write the mode to generate interrupt on match */
    CySysWdtWriteMode(WDT_COUNTER, CY_SYS_WDT_MODE_INT);
    /* Configure the WDT counter clear on a match setting */
    CySysWdtWriteClearOnMatch(WDT_COUNTER, WDT_COUNTER_ENABLE);
    /* Configure the WDT counter match comparison value */
    CySysWdtWriteMatch(WDT_COUNTER, WDT_TIMEOUT);
    /* Reset WDT counter */
    CySysWdtResetCounters(WDT_COUNTER);
    /* Enable the specified WDT counter */
    CySysWdtEnable(WDT_COUNTER_MASK);
    /* Lock out configuration changes to the Watchdog timer registers */
    CySysWdtLock();    
}


/*******************************************************************************
* Function Name: WDT_Stop
********************************************************************************
*
* Summary:
*  This API stops the WDT timer.
*
*******************************************************************************/
void WDT_Stop(void)
{
    /* Unlock the WDT registers for modification */
    CySysWdtUnlock(); 
    /* Disable the specified WDT counter */
    CySysWdtDisable(WDT_COUNTER_MASK);
    /* Locks out configuration changes to the Watchdog timer registers */
    CySysWdtLock();    
}


/*******************************************************************************
* Function Name: WriteAttrServChanged()
********************************************************************************
*
* Summary:
*   Sets serviceChangedHandle for enabling or disabling hidden service.
*
* Parameters:
*   None
*
* Return:
*   None
*
*******************************************************************************/
void WriteAttrServChanged(void)
{
    uint32 value;
    CYBLE_GATT_HANDLE_VALUE_PAIR_T    handleValuePair;
    
    /* Force client to rediscover services in range of bootloader service */
    value = (cyBle_customs[0u].customServiceHandle)<<16u|\
                                (cyBle_customs[0u].customServiceInfo[0u].customServiceCharDescriptors[0u]);
    handleValuePair.value.val = (uint8 *)&value;
    handleValuePair.value.len = sizeof(value);

    handleValuePair.attrHandle = cyBle_gatts.serviceChangedHandle;
    CyBle_GattsWriteAttributeValue(&handleValuePair, 0u, NULL,CYBLE_GATT_DB_LOCALLY_INITIATED);
}


/*******************************************************************************
* Function Name: UpdateConnectionParam
********************************************************************************
* Summary:
*        Send the Connection Update Request to Client device after connection 
* and modify theconnection interval for low power operation.
*
* Parameters:
*	void
*
* Return:
*  void
*
*******************************************************************************/
void UpdateConnectionParam(void)
{
	/* If device is connected and Update connection parameter not updated yet,
	* then send the Connection Parameter Update request to Client. */
    if(deviceConnected && isConnectionUpdateRequested)
	{
		/* Reset the flag to indicate that connection Update request has been sent */
		isConnectionUpdateRequested = FALSE;
		
		/* Send Connection Update request with set Parameter */
		CyBle_L2capLeConnectionParamUpdateRequest(connHandle.bdHandle, &ConnectionParam);
	}
}


/*******************************************************************************
* Function Name: UpdateNotificationCCCD
********************************************************************************
* Summary:
*        Update the data handle for notification status and report it to BLE 
*	component so that it can be read by Central device.
*
* Parameters:
*  void
*
* Return:
*  void
*
*******************************************************************************/
void UpdateNotificationCCCD(void)
{
	/* Local variable to store the current CCCD value */
	uint8 rawSensorDataCCCDvalue[2];
	uint8 playbackDataCCCDvalue[2];
	
	/* Handle value to update the CCCD */
	CYBLE_GATT_HANDLE_VALUE_PAIR_T rawSensorDataNotificationCCCDhandle;
	CYBLE_GATT_HANDLE_VALUE_PAIR_T playbackDataNotificationCCCDhandle;
	
	/* Update notification attribute only when there has been change in CapSense CCCD */
	if(updateRawSensorDataNotificationCCCDAttribute)
	{
		/* Reset the flag*/
		updateRawSensorDataNotificationCCCDAttribute = FALSE;
	
		/* Write the present Raw Sensor Data notification status to the local variable */
		rawSensorDataCCCDvalue[0] = sendRawSensorDataNotification;
		rawSensorDataCCCDvalue[1] = 0x00;
		
		/* Update CCCD handle with notification status data*/
		rawSensorDataNotificationCCCDhandle.attrHandle = CYBLE_SHOE_SENSOR_DATA_RAW_DATA_FIELD_CLIENT_CHARACTERISTIC_CONFIGURATION_DESC_HANDLE;
		rawSensorDataNotificationCCCDhandle.value.val = rawSensorDataCCCDvalue;
		rawSensorDataNotificationCCCDhandle.value.len = CCCD_DATA_LEN;
		
		/* Report data to BLE component for sending data when read by Central device */
		CyBle_GattsWriteAttributeValue(&rawSensorDataNotificationCCCDhandle, ZERO, &connHandle, CYBLE_GATT_DB_LOCALLY_INITIATED);
	}
	
	/* Update notification attribute only when there has been change in CapSense CCCD */
	if(updatePlaybackDataNotificationCCCDAttribute)
	{
		/* Reset the flag*/
		updatePlaybackDataNotificationCCCDAttribute = FALSE;
	
		/* Write the present Playback Data notification status to the local variable */
		playbackDataCCCDvalue[0] = sendPlaybackDataNotification;
		playbackDataCCCDvalue[1] = 0x00;
		
		/* Update CCCD handle with notification status data*/
		playbackDataNotificationCCCDhandle.attrHandle = CYBLE_RECORD_PLAYBACK_PLAYBACK_DATA_CLIENT_CHARACTERISTIC_CONFIGURATION_DESC_HANDLE;
		playbackDataNotificationCCCDhandle.value.val = playbackDataCCCDvalue;
		playbackDataNotificationCCCDhandle.value.len = CCCD_DATA_LEN;
		
		/* Report data to BLE component for sending data when read by Central device */
		CyBle_GattsWriteAttributeValue(&playbackDataNotificationCCCDhandle, ZERO, &connHandle, CYBLE_GATT_DB_LOCALLY_INITIATED);
	}
	
	
}




/* [] END OF FILE */
