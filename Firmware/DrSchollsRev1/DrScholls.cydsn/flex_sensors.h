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

#if !defined(flex_sensors_H)
#define flex_sensors_H
	
#include <project.h>


#define FLEX_SENSORS_NUM 3
	
#define FLEX_SENSORS_VALUE_HIGH ( (int)(0x0FFF*0.99) )
#define FLEX_SENSORS_VALUE_LOW  ( (int)(0x0FFF*0.5) )
	
//Above this means the sensor is open  
#define FLEX_SENSORS_OPEN_SENSOR 0x6FE00
	
#define FLEX_SENSORS_MAX_INIT 2	//Value to scale the baseline to guess on where the sensor max is

	
#define FLEX_SENSORS_MOVE_DELTA (30*FLEX_SENSORS_NUM*256)

	
//Define flags
#define FLEX_SENSORS_FLAGS_MOVEMENT 0x0001
	
#define FLEX_SENSORS_TEST_RESULT_LEFT 0x601
#define FLEX_SENSORS_TEST_RESULT_RIGHT 0x403
	
#define FLEX_SENSORS_STILL_COUNT_MAX 60*1000 //60 seconds * 1000 samples per second


#define FLEX_SENSORS_MEDIAL_FACTOR ( (int)(128*0.8) )	
#define FLEX_SENSORS_LATERAL_FACTOR ( (int)(128*0.9) )
#define FLEX_SENSORS_HEEL_FACTOR ( (int)(128*1.3) )	
	
/*******************************************************************************
* Function Name: flex_sensors_init
********************************************************************************
* Summary:
*        Stores initial baseline of sensors, estabilishes approximate max
*
* Parameters:
*  void
*
* Return:
*  void
*

*******************************************************************************/
void flex_sensors_init( void );

/*******************************************************************************
* Function Name: flex_sensors_orientation
********************************************************************************
* Summary:
*        Returns orientation
*
* Parameters:
*  void
*
* Return:
*  Orientation
*

*******************************************************************************/
int flex_sensors_orientation( void );



/*******************************************************************************
* Function Name: flex_sensors_test
********************************************************************************
* Summary:
*        Tests whether sensors are connected and to which common
*        Orientation is (bits 7-0 indicate which channels are present to common 0, bits 15-8 indicate which channels are present to common 1)
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_test( void );

/*******************************************************************************
* Function Name: flex_sensors_measure_all
********************************************************************************
* Summary:
*        Measures all sensors
*
* Parameters:
*  Array to put results into
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_measure_all(int array[]);



/*******************************************************************************
* Function Name: flex_sensors_measure_single
********************************************************************************
* Summary:
*        Measures a single sensor. Assumes the analog system is already powered up
*
* Parameters:
*  Sensor number
*
* Return:
*  int result
*

*******************************************************************************/
int flex_sensors_measure_single(int sensorNum);

/*******************************************************************************
* Function Name: flex_sensors_force_baseline
********************************************************************************
* Summary:
*        Resets initial baseline of sensors, estabilishes approximate max
*
* Parameters:
*  void
*
* Return:
*  void
*

*******************************************************************************/
void flex_sensors_force_baseline( void );

/*******************************************************************************
* Function Name: flex_sensors_manage_limits
********************************************************************************
* Summary:
*        Updates baselines and maxes
*
* Parameters:
*  Array of sensor values
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_manage_limits(int values[]);

/*******************************************************************************
* Function Name: flex_sensors_manage_peaks
********************************************************************************
* Summary:
*        Updates peaks from last BLE communication
*
* Parameters:
*  Array of peaks, Array of sensor values
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_manage_peaks(int peaks[], int values[]);

/*******************************************************************************
* Function Name: flex_sensors_manage_peaks
********************************************************************************
* Summary:
*        Updates peaks from last BLE communication
*
* Parameters:
*  Array of peaks, Array of sensor values
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_reset_peaks(int peaks[]);


/*******************************************************************************
* Function Name: flex_sensors_convert_scaled
********************************************************************************
* Summary:
*        Scales flex sensor values based on baseline and max
*
* Parameters:
*  Array to put scalled sensor values in, Array of unscaled sensor values
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_convert_scaled(uint8 scaled[], int unscaled[]);


/*******************************************************************************
* Function Name: flex_sensors_is_moving
********************************************************************************
* Summary:
*        Checks if movement flag is set
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
int flex_sensors_is_moving( void );


/*******************************************************************************
* Function Name: flex_sensors_pwr_up
********************************************************************************
* Summary:
*        Powers up analog system
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_pwr_up( void );

/*******************************************************************************
* Function Name: flex_sensors_pwr_dn
********************************************************************************
* Summary:
*        Powers dn analog system
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_pwr_dn( void );

#endif /* flex_sensors_H */
/* [] END OF FILE */
