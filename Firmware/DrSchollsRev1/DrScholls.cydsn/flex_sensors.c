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

#include <flex_sensors.h>
#include <stdlib.h>
//#include "debug.h"


uint8 flex_sensors_idac[FLEX_SENSORS_NUM];
int flex_sensors_baseline[FLEX_SENSORS_NUM];
int flex_sensors_max[FLEX_SENSORS_NUM];
int flex_sensors_hist[FLEX_SENSORS_NUM];
#define FLEX_SENSORS_AVG_SIZE 8
static int flex_sensors_avg[FLEX_SENSORS_NUM][FLEX_SENSORS_AVG_SIZE];

uint32 flex_sensors_flags = 0;

int orientation = 0;

uint32 flex_sensors_still_count = 0;

/*******************************************************************************
* Function Name: flex_sensors_init
********************************************************************************
* Summary:
*        Stores initial baseline of sensors, estabilishes approximate max
*
* Parameters:
*  None
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_init( void ) {
	int i;
	int flex_sensors_values[FLEX_SENSORS_NUM];
	
	ADC_Start();
	IDAC_Start();
	AMux_Start();
	
	
	//Initialize current
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		flex_sensors_idac[i] = 0x80; //Midscale	
	}

	orientation = 0;
	flex_sensors_measure_all(flex_sensors_values); //First measurements are bogus
	flex_sensors_measure_all(flex_sensors_values);
	
	//Initialize baseline and max's
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		flex_sensors_baseline[i] = flex_sensors_values[i];
		flex_sensors_max[i] = flex_sensors_baseline[i]*FLEX_SENSORS_MAX_INIT;
		flex_sensors_hist[i] = flex_sensors_values[i];
	}
	
	
}


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
int flex_sensors_orientation( void ) {
	
	return orientation;
	
}

/*******************************************************************************
* Function Name: flex_sensors_test
********************************************************************************
* Summary:
*        Tests whether sensors are connected and to which common
*
* Parameters:
*  None
*
* Return:
*  int test result (bits 7-0 indicate which channels are present to common 0, bits 15-8 indicate which channels are present to common 1)
*

*******************************************************************************/
void flex_sensors_test( void ) {
	int sensor_values[FLEX_SENSORS_NUM];
	
	int j, i;
	int retval = 0;
			
	for (j=0;j<2;j++) { //Number of commons
				
		if (j == 0) {
			*(reg32 *)(Sensor_0_DR) |= 0x11;  //tri-state both commons
			*(reg32 *)(Sensor_0_DR) &= ~(0x01);  //Set for common 0
		} else {
			*(reg32 *)(Sensor_0_DR) |= 0x11;  //tri-state both commons
			*(reg32 *)(Sensor_0_DR) &= ~(0x10);  //Set for common 1
		}
		
		CyDelayUs(50);
		
		orientation = 0;
	
		flex_sensors_pwr_up();
	
		for (i=0;i<FLEX_SENSORS_NUM;i++) {
			sensor_values[i] = flex_sensors_measure_single(i);
		}

		flex_sensors_pwr_dn();
	
		if (sensor_values[0] < FLEX_SENSORS_OPEN_SENSOR) {
			retval |= 1<<(8*j);
		}
		
		if (sensor_values[1] < FLEX_SENSORS_OPEN_SENSOR) {
			retval |= 2<<(8*j);
		}
		
		if (sensor_values[2] < FLEX_SENSORS_OPEN_SENSOR) {
			retval |= 4<<(8*j);
		}
		
	}
    
	*(reg32 *)(Sensor_0_DR) &= ~(0x11);  //enable both commons
	
	orientation = retval;
	
}



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
void flex_sensors_measure_all(int array[]) {
	int i;
	int delta_sum;
	int scale_factor[FLEX_SENSORS_NUM] = {	FLEX_SENSORS_MEDIAL_FACTOR, 
											FLEX_SENSORS_LATERAL_FACTOR, 
											FLEX_SENSORS_HEEL_FACTOR};
	
	flex_sensors_pwr_up();
	
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
	
		if (flex_sensors_orientation() == FLEX_SENSORS_TEST_RESULT_RIGHT) {
			//Right shoe
			//array[i] = (scale_factor[i] * flex_sensors_measure_single( (FLEX_SENSORS_NUM-1) - i) ) / 128;
			array[i] = flex_sensors_measure_single( (FLEX_SENSORS_NUM-1) - i);
		} else {
			//Left shoe
			//array[i] = (scale_factor[i] * flex_sensors_measure_single(i) ) / 128;
			array[i] = flex_sensors_measure_single(i);
		}
	}
		
	flex_sensors_pwr_dn();
	
	//Check for movement
	delta_sum = 0;
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		if (array[i] < FLEX_SENSORS_OPEN_SENSOR) {
			delta_sum += abs(flex_sensors_hist[i] - array[i]);
			
			if (array[i] > flex_sensors_hist[i]) {
				flex_sensors_hist[i]++;
			} else if (array[i] < flex_sensors_hist[i]) {
				flex_sensors_hist[i]--;
			}
			
		} else {
			//If open sensor replace with last known history sample
			//array[i] = flex_sensors_hist[i];
		}
	}
	
	//if (delta_sum > 0) {
	//	DBG_PRINTF("array: %d %d %d delta sum: %d\r\n", array[0]/256, array[1]/256, array[2]/256, delta_sum);
	//}
	
	if (delta_sum > FLEX_SENSORS_MOVE_DELTA) {
		flex_sensors_flags |= FLEX_SENSORS_FLAGS_MOVEMENT;
		flex_sensors_still_count = 0;
	} else if (flex_sensors_still_count < FLEX_SENSORS_STILL_COUNT_MAX) {
		flex_sensors_still_count++;
	}
	
	
	
}



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
int flex_sensors_measure_single(int sensorNum) {
	
	int result = 0;
	//int resample = 1;
	
	AMux_Select(sensorNum);	
	
	//while (resample == 1) {
		//IDAC_SetValue(flex_sensors_idac[sensorNum]); //Update current
		IDAC_SetValue(8); //Update current
		CyDelayUs(50);	//Settling time delay
		ADC_StartConvert();
		ADC_IsEndConversion(ADC_WAIT_FOR_RESULT);	//Stall until complete
		result = ADC_GetResult16(0);
		//resample = 0;
		if (result > FLEX_SENSORS_VALUE_HIGH && flex_sensors_idac[sensorNum] > 1) {
			flex_sensors_idac[sensorNum]--;
			//resample = 1;
		}
		if (result < FLEX_SENSORS_VALUE_LOW && flex_sensors_idac[sensorNum] < 0xFF) {
			flex_sensors_idac[sensorNum]++;
			//resample = 1;
		}
	//}
    
	return (result<<8); // / flex_sensors_idac[sensorNum];	
	//Conversion equation
	//sensor_value = 524288 / (1.024 / sensor_resistance / 0.0000012);
	
}

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
void flex_sensors_force_baseline( void ) {
	int i;	
	
	//Initialize baseline and max's
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		flex_sensors_baseline[i] = flex_sensors_hist[i];
		flex_sensors_max[i] = flex_sensors_hist[i]*FLEX_SENSORS_MAX_INIT;
	}
	
}

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
void flex_sensors_manage_limits(int values[]) {
	int i;
	
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		
		//First check if sensor is open or shorted
		if ( (values[i] > 100) && (values[i] < ( FLEX_SENSORS_VALUE_HIGH<<8) ) ) {

			
			if (flex_sensors_still_count == FLEX_SENSORS_STILL_COUNT_MAX) {
				
				//if still for a while then creep up baseline
				if (flex_sensors_baseline[i] < values[i]) {
					flex_sensors_baseline[i]++;
				}
				
				
				//try to get sensors to sit at 2x baseline, but if values are greater then let it increase				
				if (flex_sensors_max[i] > flex_sensors_baseline[i]*FLEX_SENSORS_MAX_INIT) {
					flex_sensors_max[i]--;
				} else if (flex_sensors_max[i] < flex_sensors_baseline[i]*FLEX_SENSORS_MAX_INIT) {
					flex_sensors_max[i]++;
				}
			}
			
			
			if (values[i] < flex_sensors_baseline[i])
				flex_sensors_baseline[i] = values[i];
			
			if (values[i] > flex_sensors_max[i])
				flex_sensors_max[i] = values[i];
			
			
		}
	}
	
}

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
void flex_sensors_manage_peaks(int peaks[], int values[]) {
	int i, j;
	int32 sum;
	
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		sum = 0;
		for (j=FLEX_SENSORS_AVG_SIZE-1;j>0;j--) {
			flex_sensors_avg[i][j] = flex_sensors_avg[i][j-1];
			sum += flex_sensors_avg[i][j];
		}
		flex_sensors_avg[i][0] = values[i];
		sum += values[i];
		peaks[i] = sum / FLEX_SENSORS_AVG_SIZE;
	}
	
}

/*******************************************************************************
* Function Name: flex_sensors_reset_peaks
********************************************************************************
* Summary:
*        Resets peaks from last BLE communication
*
* Parameters:
*  Array of peaks, Array of sensor values
*
* Return:
*  None
*

*******************************************************************************/
void flex_sensors_reset_peaks(int peaks[]) {
	int i;
	
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		peaks[i] = 0;
	}
	
}

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
void flex_sensors_convert_scaled(uint8 scaled[], int unscaled[]) {
	int i;
	int scaling[FLEX_SENSORS_NUM];
	
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		if (flex_sensors_max[i] <= flex_sensors_baseline[i]) {
			
			scaled[i] = 0;
			
		} else {
			scaling[i] = (unscaled[i] - flex_sensors_baseline[i]) * 256 / (flex_sensors_max[i] - flex_sensors_baseline[i]);
			
			if (scaling[i] < 0)
				scaling[i] = 0;
				
			if (scaling[i] > 255)
				scaling[i] = 255;
			
			scaled[i] = scaling[i];
		}
	}
}



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
int flex_sensors_is_moving( void ) {
	int r = (flex_sensors_flags & FLEX_SENSORS_FLAGS_MOVEMENT);
	
	//Clear flag
	flex_sensors_flags &= ~FLEX_SENSORS_FLAGS_MOVEMENT;
	return r;
}


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
void flex_sensors_pwr_up( void ) {
	
	ADC_Wakeup();
	IDAC_Wakeup();
	
	CyDelayUs(10);
	
}

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
void flex_sensors_pwr_dn( void ) {
	
	ADC_Sleep();
	IDAC_Sleep();
	
}


/* [] END OF FILE */
