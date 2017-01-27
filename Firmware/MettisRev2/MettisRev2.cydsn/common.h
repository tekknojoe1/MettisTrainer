/*******************************************************************************
* File Name: common.h
*
* Version 1.0
*
* Description:
*  Contains the function prototypes and constants used by the example project.
*
********************************************************************************
* Copyright 2014, Cypress Semiconductor Corporation.  All rights reserved.
* You may use this file only in accordance with the license, terms, conditions,
* disclaimers, and limitations in the end user license agreement accompanying
* the software package with which this file was provided.
*******************************************************************************/

#include <project.h>
#include <stdio.h>


/***************************************
* API Constants
***************************************/

/* Possilble LED states */
#define LED_ON                              (0u)
#define LED_OFF                             (1u)

#define DISCONNECTED                        (0u)
#define ADVERTISING                         (1u)
#define CONNECTED                           (2u)

#define ENABLED                             (1u)
#define DISABLED                            (0u)

/* Delay value to produce blinking LED */
#define BLINK_DELAY                         (2000u)

#define PACE_TIMER_VALUE                    (333u)

#define NOTIFICATION_TIMER_VALUE            (100u)

#define WALKING_PROFILE_TIMER_VALUE         (33u)
#define RUNNING_PROFILE_TIMER_VALUE         (17u)


#define WDT_COUNTER                 (CY_SYS_WDT_COUNTER1)
#define WDT_COUNTER_MASK            (CY_SYS_WDT_COUNTER1_MASK)
#define WDT_INTERRUPT_SOURCE        (CY_SYS_WDT_COUNTER1_INT) 
#define WDT_COUNTER_ENABLE          (1u)
#define WDT_1SEC                    (32767u)
#define WDT_500MSEC                 (16383u)
#define WDT_250MSEC                 (8191u)
#define WDT_100MSEC                 (3277u)


/***************************************
*        Function Prototypes
***************************************/
CY_ISR_PROTO(ButtonPressInt);


/* [] END OF FILE */
