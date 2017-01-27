/*******************************************************************************
* File Name: main.h
*
* Version: 1.40
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

#include <project.h>
#include <stdio.h>
#include "Options.h"
#include "debug.h"
#include "OTAMandatory.h"
#include "OTAOptional.h"

/* General Macros */
#define TRUE							1
#define FALSE							0
#define ZERO							0
/****************************************************************************/

#define LED_TIMEOUT                         (2047u)   /* Сounts of 1 millisecond */
#define LED_ON                              (0u)
#define LED_OFF                             (1u)

#define BLE_PACKET_SIZE_MAX                 (300u)

/*WDT setup values*/
#define WDT_COUNTER                         (CY_SYS_WDT_COUNTER1)
#define WDT_COUNTER_MASK                    (CY_SYS_WDT_COUNTER1_MASK)
#define WDT_INTERRUPT_SOURCE                (CY_SYS_WDT_COUNTER1_INT) 
#define WDT_COUNTER_ENABLE                  (1u)
#define WDT_TIMEOUT_CONNECTED               (1*32767u/1000u) /* 1 ms @ 32.768kHz clock */
#define WDT_TIMEOUT_UNCONNECTED             (10*32767u/1000u) /* 10 ms @ 32.768kHz clock */
#define WDT_TIMEOUT_SLEEP                   (100*32767u/1000u) /* 1000 ms @ 32.768kHz clock */

#define AWAKE_TIMEOUT						3000 //30 seconds at 10 ms ticks

#define SERIAL_NUMBER                       ("123456")

#define ENABLE_BTS                          (1u)
#define DISABLE_BTS                         (0u)


/***************Function Declarations*****************************/
void InitializeSystem(void);
void ChangeBootloaderServiceState(uint32 enabledState);
void HandleRawData(void);
void HandlePlaybackData(void);
int SendDataOverRawSensorNotification(uint8 rawSensorData[]);
int SendDataOverRecordPlaybackNotification(uint8 playbackData[]);
/****************************************************************************/


/* [] END OF FILE */
