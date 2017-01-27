/* ========================================
 *
 * Copyright YOUR COMPANY, THE YEAR
 * All Rights Reserved
 * UNPUBLISHED, LICENSED SOFTWARE.
 *
 * CONFIDENTIAL AND PROPRIETARY INFORMATION
 * WHICH IS THE PROPERTY OF your company.
 *
 * ========================================
*/

#if !defined(mettis_H)
#define mettis_H

#include <project.h>	
	

	
#define HEEL 2
#define MEDIAL 0  //Toe
#define LATERAL 1 //Pinky
	
//User must walk between the low and high cadence (per foot) for them to calibrate
#define SAMPLES_PER_SECOND 100  //Set by the watchdog timer interval	
	
#define SENSOR_IMPACT_RATIO 0.5 //The upper threshold for impact based on the average
#define SENSOR_IMPACT_MAX_TIME 5000 //Maximum number of samples for a step in ms
#define SENSOR_IMPACT_MAX_COUNT  (SENSOR_IMPACT_MAX_TIME * SAMPLES_PER_SECOND / 1000) 
#define SENSOR_IMPACT_MIN_THRESHOLD   (4 * 4096)
#define SENSOR_LIFT_OFF_RATIO 0.25 //The upper threshold for impact based on the average
#define SENSOR_LIFT_OFF_MIN_THRESHOLD (1 * 4096)
	
	
	



	
//Cadence is in steps per minute
#define CADENCE_CAL_LOW_CADENCE 25
#define CADENCE_CAL_LOW_SAMPLES (SAMPLES_PER_SECOND * 60 / CADENCE_CAL_LOW_CADENCE)

#define CADENCE_CAL_HIGH_CADENCE 75
#define CADENCE_CAL_HIGH_SAMPLES (SAMPLES_PER_SECOND * 60 / CADENCE_CAL_HIGH_CADENCE)
	
//Contact time is the amount of time we are on the ground in 2ms increments
#define CONTACT_TIME_MS_PER_COUNT 10
#define CONTACT_TIME_MS_PER_SAMPLE (1000 / SAMPLES_PER_SECOND / CONTACT_TIME_MS_PER_COUNT)

#define CONTACT_TIME_MAX_CONTACT (256 * CONTACT_TIME_MS_PER_SAMPLE)
	
#define AIR_TIME_MS_PER_COUNT 10
#define AIR_TIME_MS_PER_SAMPLE (1000 / SAMPLES_PER_SECOND / AIR_TIME_MS_PER_COUNT)
#define AIR_TIME_MAX_SAMPLES (256 * AIR_TIME_MS_PER_SAMPLE)

//Maximum heel2toe time 
#define HEEL2TOE_MAX_SAMPLES 255
	
//Number of steps until calibration will take place	
#define CADENCE_CAL_STEPS_THRESH 12
	
#define HIST_SIZE CADENCE_CAL_STEPS_THRESH

//Not reporting a cadence less than 25, it will be 0
#define CADENCE_MIN_CADENCE CADENCE_CAL_LOW_CADENCE
#define CADENCE_MAX_SAMPLES (SAMPLES_PER_SECOND * 60 / CADENCE_MIN_CADENCE)
	
//Mettis state bit declarations
#define METTIS_STATE_RECORD_BITS 0xE0
#define METTIS_STATE_RECORD_SHIFT 5
	
//Cal flags
#define METTIS_CAL_FORCE_MAX 0x8
#define METTIS_CAL_FORCE_MIN 0x4
#define METTIS_CAL_MAX_COMPLETE 0x2
#define METTIS_CAL_MIN_COMPLETE 0x1
#define METTIS_CAL_COMPLETE	 (METTIS_CAL_MAX_COMPLETE | METTIS_CAL_MIN_COMPLETE)

	
//#define METTIS_ORDER2_COEF 419
//#define METTIS_ORDER1_COEF 
	
#define IMPACT_FORCE_100_PERCENT 64
	
	
/*******************************************************************************
* Function Name: mettis_init
********************************************************************************
* Summary:
*        Initializes state machine and averages
*
* Parameters:
*  void
*
* Return:
*  void
*

*******************************************************************************/
void mettis_init( void );

/*******************************************************************************
* Function Name: mettis_sensor_sm
********************************************************************************
* Summary:
*        Executes mettis sensor state machine
*
* Parameters:
*  sensor_num : contains which sensor on the foot to process
*  sensor_value : contains current sensor value
*
* Return:
*  void
*

*******************************************************************************/
void mettis_sensor_sm(int sensor_num, int sensor_value);


/*******************************************************************************
* Function Name: mettis_task
********************************************************************************
* Summary:
*        Executes mettis state machines
*
* Parameters:
*  void
*
* Return:
*  void
*

*******************************************************************************/
void mettis_task(int sensor_peaks[], int sensor_values[]);

/*******************************************************************************
* Function Name: mettis_get_state
********************************************************************************
* Summary:
*  Returns mettis state
*
* Parameters:
*
* Return:
*  None
*

*******************************************************************************/
uint8 mettis_get_state( void );

/*******************************************************************************
* Function Name: mettis_get_flags
********************************************************************************
* Summary:
*  Returns mettis flags
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
uint8 mettis_get_flags( void );






/*******************************************************************************
* Function Name: mettis_force_cal_max
********************************************************************************
* Summary:
*  Sets the mettis_state bits with the record state
*
* Parameters:
*  Record state
*
* Return:
*  None
*

*******************************************************************************/
void mettis_force_cal_max( void );


/*******************************************************************************
* Function Name: mettis_force_cal_max
********************************************************************************
* Summary:
*  Forces max calibration
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
void mettis_force_cal_max( void );


/*******************************************************************************
* Function Name: mettis_force_cal_min
********************************************************************************
* Summary:
*  Forces min calibration
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
void mettis_force_cal_min( void );


/*******************************************************************************
* Function Name: mettis_convert_scaled
********************************************************************************
* Summary:
*        Scales flex sensor values based on calibration min and max
*
* Parameters:
*  Array to put scalled sensor values in, Array of unscaled sensor values
*
* Return:
*  None
*

*******************************************************************************/
void mettis_convert_scaled(uint8 scaled[], int unscaled[]);


/*******************************************************************************
* Function Name: mettis_get_cadence
********************************************************************************
* Summary:
*        Scales flex sensor values based on calibration min and max.
*
* Parameters:
*  None
*
* Return:
*  number of calibration steps
*

*******************************************************************************/
uint8 mettis_get_cadence( void );

/*******************************************************************************
* Function Name: mettis_get_total_steps
********************************************************************************
* Summary:
*  Returns total steps measured
*
* Parameters:
*  None
*
* Return:
*  total steps
*

*******************************************************************************/
uint32 mettis_get_total_steps( void );

/*******************************************************************************
* Function Name: mettis_get_contact_time
********************************************************************************
* Summary:
*  Returns contact time in 2ms per count
*
* Parameters:
*  None
*
* Return:
*  contact time
*

*******************************************************************************/
uint8 mettis_get_contact_time( void );

/*******************************************************************************
* Function Name: mettis_get_air_time
********************************************************************************
* Summary:
*  Returns contact time in ms per count
*
* Parameters:
*  None
*
* Return:
*  contact time
*

*******************************************************************************/
uint8 mettis_get_air_time( void );

/*******************************************************************************
* Function Name: mettis_get_heel2toe
********************************************************************************
* Summary:
*  Returns time between heel to toe impact
*
* Parameters:
*  None
*
* Return:
*  heel2toe
*

*******************************************************************************/
uint8 mettis_get_heel2toe( void );

/*******************************************************************************
* Function Name: mettis_get_impact_force
********************************************************************************
* Summary:
*  Returns the amount above the calibrated max 
*
* Parameters:
*  None
*
* Return:
*  impact_force
*

*******************************************************************************/
uint8 mettis_get_impact_force( void );

/*******************************************************************************
* Function Name: mettis_get_pronation
********************************************************************************
* Summary:
*  Returns the prontaion amount. Up to 100% is overpronation, -100% is underpronation
*
* Parameters:
*  None
*
* Return:
*  pronation
*

*******************************************************************************/
uint8 mettis_get_pronation( void );


/*******************************************************************************
* Function Name: mettis_get_cal_steps
********************************************************************************
* Summary:
*  Returns the number of cal_steps
*
* Parameters:
*  None
*
* Return:
*  number of calibration steps
*

*******************************************************************************/
uint8 mettis_get_cal_steps( void );




#endif /* mettis_H */
/* [] END OF FILE */
