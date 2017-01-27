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

#include "flex_sensors.h"
#include "mettis.h"
#include "record.h"
#include "debug.h"

static int32 sensor_ave[FLEX_SENSORS_NUM];
static int32 sensor_impact_threshold[FLEX_SENSORS_NUM];
static int32 sensor_lift_off_threshold[FLEX_SENSORS_NUM];
	

#define SENSOR_INIT 4
#define SENSOR_IDLE 0
#define SENSOR_LIFT_OFF 1
#define SENSOR_IMPACT 2
#define SENSOR_LAND 3
static int sensor_state[FLEX_SENSORS_NUM];

static int sensor_max[FLEX_SENSORS_NUM][HIST_SIZE];  //Stores peaks of last few steps
static int sensor_min[FLEX_SENSORS_NUM][HIST_SIZE];  //Stores mins of last few steps

static int local_max[FLEX_SENSORS_NUM];
static int local_min[FLEX_SENSORS_NUM];
static int local_max_sum[FLEX_SENSORS_NUM];
static int local_counter[FLEX_SENSORS_NUM];

static int sensor_delay_counter[FLEX_SENSORS_NUM];

#define CADENCE_INIT 4
#define CADENCE_IDLE 0
#define CADENCE_IMPACT 1
#define CADENCE_LAND 2
#define CADENCE_AIR 3
static int cadence_state;

static int cadence_counter = 0;
static int cal_steps = 0;
static int cadence_samples = 0;
static uint32 total_steps = 0;

static int contact_time_counter = 0;
static int contact_time_samples = 0;

static int air_time_counter = 0;
static int air_time_samples = 0;

static int heel2toe_counter = 0;
static int heel2toe_samples = 0;

static int cal_max[FLEX_SENSORS_NUM];
static int cal_min[FLEX_SENSORS_NUM];

static int cal_flags = 0; //No calibration has been done


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
void mettis_init( void ) {
	int i;
	
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		sensor_state[i] = SENSOR_INIT;
		sensor_ave[i] = 0;
	}
	
	
	
	cadence_state = CADENCE_INIT;
}

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
void mettis_sensor_sm(int sensor_num, int sensor_value) {
	int i;
	
	//Update average
	if (sensor_value > sensor_ave[sensor_num]) {
		sensor_ave[sensor_num]++;
	} else if (sensor_value < sensor_ave[sensor_num]) {
		sensor_ave[sensor_num]--;
	}
	
	if ( (sensor_ave[sensor_num] * SENSOR_IMPACT_RATIO) < SENSOR_IMPACT_MIN_THRESHOLD) {
        sensor_impact_threshold[sensor_num] = sensor_ave[sensor_num] + SENSOR_IMPACT_MIN_THRESHOLD;
	} else {
        sensor_impact_threshold[sensor_num] = sensor_ave[sensor_num] * (1 + SENSOR_IMPACT_RATIO);
   	}
	
	if ( (sensor_ave[sensor_num] * SENSOR_LIFT_OFF_RATIO) < SENSOR_LIFT_OFF_MIN_THRESHOLD) {
      	sensor_lift_off_threshold[sensor_num] = sensor_ave[sensor_num] + SENSOR_LIFT_OFF_MIN_THRESHOLD;
	} else {
        sensor_lift_off_threshold[sensor_num] = sensor_ave[sensor_num] * (1 + SENSOR_LIFT_OFF_RATIO);
   	}
    
		
	
	if (sensor_num == 0) {
		
		//DBG_PRINT_DEC(sensor_value);
		//DBG_PRINT_DEC(sensor_ave[sensor_num]);
		//DBG_PRINT_TEXT("\r\n");
	
		
		
	}
	
	
	if (sensor_value > local_max[sensor_num]) {
		local_max[sensor_num] = sensor_value;
	}
	
	if (sensor_value < local_min[sensor_num]) {
		local_min[sensor_num] = sensor_value;
	}
	
	switch (sensor_state[sensor_num]) {
	
		case SENSOR_INIT:
			sensor_ave[sensor_num] = sensor_value;  //Reset average
			sensor_state[sensor_num] = SENSOR_IDLE;
		break;
			
		case SENSOR_IMPACT:
					
			//Advance history buffers
			for (i=1;i<HIST_SIZE;i++) {
				sensor_max[sensor_num][HIST_SIZE-i] = sensor_max[sensor_num][HIST_SIZE-i-1];
				sensor_min[sensor_num][HIST_SIZE-i] = sensor_min[sensor_num][HIST_SIZE-i-1];
			}
			
			//Max is the average during the landing state
			if (local_counter[sensor_num] > 0) {
				sensor_max[sensor_num][0] = local_max_sum[sensor_num] / local_counter[sensor_num];
			}
			//Min is the minimum we saw in the air/idle state
			sensor_min[sensor_num][0] = local_min[sensor_num];
			
			local_min[sensor_num] = 2147483647;
			local_max[sensor_num] = sensor_value;
			local_max_sum[sensor_num] = 0;
			local_counter[sensor_num] = 0;
			
			
			sensor_state[sensor_num] = SENSOR_LAND;
		break;
			
		case SENSOR_LAND:
			
			if (local_counter[sensor_num] < SENSOR_IMPACT_MAX_COUNT) {
				local_counter[sensor_num]++;
				local_max_sum[sensor_num] += sensor_value;
			}
			
			if (sensor_value < sensor_lift_off_threshold[sensor_num]) {
				sensor_delay_counter[sensor_num] = 3 * local_counter[sensor_num] / 4;
				sensor_state[sensor_num] = SENSOR_LIFT_OFF;
			}
			
			
			
		break;
			
		case SENSOR_LIFT_OFF:
			
			if (sensor_delay_counter[sensor_num] == 0) {
				sensor_state[sensor_num] = SENSOR_IDLE;
			} else {
				sensor_delay_counter[sensor_num]--;
			}
			
			
		break;	
					
		default: //SENSOR_IDLE (sensor in air)
			
			if (sensor_value > sensor_impact_threshold[sensor_num]) {
				sensor_state[sensor_num] = SENSOR_IMPACT;
				
			}
		break;
	}
	
	
	
	
	
}


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
void mettis_task(int sensor_peaks[], int sensor_values[]) {
	int i, j;
	int ave_max[FLEX_SENSORS_NUM];
	int ave_min[FLEX_SENSORS_NUM];
	uint8 record_buf[RECORD_DATA_SIZE];
	
	//Measure sensors
	flex_sensors_measure_all(sensor_values);
	flex_sensors_manage_limits(sensor_values);
	flex_sensors_manage_peaks(sensor_peaks, sensor_values);
	
	if (record_isRecording() ) {
		record_buf[0] = (sensor_values[0]>>16) & 0xFF;
		record_buf[1] = (sensor_values[0]>>8) & 0xFF;
		record_buf[2] = (sensor_values[1]>>16) & 0xFF;
		record_buf[3] = (sensor_values[1]>>8) & 0xFF;
		record_buf[4] = (sensor_values[2]>>16) & 0xFF;
		record_buf[5] = (sensor_values[2]>>8) & 0xFF;
		record_post_data(record_buf);
	}
	
	mettis_sensor_sm(HEEL, sensor_values[HEEL]);
	mettis_sensor_sm(MEDIAL, sensor_values[MEDIAL]);
	mettis_sensor_sm(LATERAL, sensor_values[LATERAL]);
	
	if (cadence_counter < CADENCE_MAX_SAMPLES) {
		cadence_counter++;
	}
	
	switch (cadence_state) {
		
		case CADENCE_INIT:
			
			//Initialize mins and max's
			for (i=0;i<FLEX_SENSORS_NUM;i++) {
				cal_min[i] = sensor_values[i];
				cal_max[i] = cal_min[i]*FLEX_SENSORS_MAX_INIT;
			}
		
			cadence_state = CADENCE_IDLE;
		break;
					
		case CADENCE_IMPACT:
		
			if ( (cadence_counter < CADENCE_CAL_LOW_SAMPLES) && (cadence_counter > CADENCE_CAL_HIGH_SAMPLES) ) {
				cal_steps++;
			} else {
				cal_steps = 0;
			}
			
			cadence_samples = cadence_counter;
			
			cadence_counter = 0;
			contact_time_counter++;
			cadence_state = CADENCE_LAND;
			contact_time_samples = contact_time_counter;
			air_time_samples = air_time_counter;
			total_steps++;
			
		break;
			
		case CADENCE_LAND:
		
			if ( 	(sensor_state[HEEL] != SENSOR_IMPACT) && (sensor_state[HEEL] != SENSOR_LAND) && 
					(sensor_state[MEDIAL] != SENSOR_IMPACT) && (sensor_state[MEDIAL] != SENSOR_LAND) ) {
				cadence_state = CADENCE_AIR;
			}
					
			if (contact_time_samples < CONTACT_TIME_MAX_CONTACT) {
				contact_time_counter++;
			}	
			
			if (sensor_state[MEDIAL] == SENSOR_IDLE) {
                heel2toe_counter++;
            }
			
			air_time_counter = 1;
		break;
	
							
		case CADENCE_AIR:
			
			air_time_counter++;
			contact_time_samples = contact_time_counter;
			cadence_state = CADENCE_IDLE;	
			
			heel2toe_samples = heel2toe_counter;
		break;
		
		default: //CADENCE_IDLE
			
			if (air_time_counter < AIR_TIME_MAX_SAMPLES) {
				air_time_counter++;
			}
			
			//Wait for landing of any sensor
			if ( (sensor_state[HEEL] == SENSOR_IMPACT) || (sensor_state[MEDIAL] == SENSOR_IMPACT) ) {
				cadence_state = CADENCE_IMPACT;
				contact_time_counter = 1;
				heel2toe_counter = 0;
			}
		break;
	}
	
	
	if ( (cal_flags & METTIS_CAL_FORCE_MAX) > 0) {
		cal_flags &= ~METTIS_CAL_FORCE_MAX;  //clear flag
		
		for (j=0;j<FLEX_SENSORS_NUM;j++) {
			cal_max[j] = 2*sensor_values[j];
			//cal_max[j] = cal_min[j]*288/256;
		}
		
		cal_flags |= METTIS_CAL_MAX_COMPLETE;
	}
	
	if ( (cal_flags & METTIS_CAL_FORCE_MIN) > 0) {
		cal_flags &= ~METTIS_CAL_FORCE_MIN;  //clear flag
		
		for (j=0;j<FLEX_SENSORS_NUM;j++) {
			cal_min[j] = sensor_values[j];
		}
		
		cal_flags |= METTIS_CAL_MIN_COMPLETE;
	}
	
	//Drift the minimum
	for (j=0;j<FLEX_SENSORS_NUM;j++) {
		if (sensor_values[j] < cal_min[j]) {
			cal_min[j]--;
		}
	}
	
	
	if (cal_steps > CADENCE_CAL_STEPS_THRESH) {
				
		//Average max and mins and set the mins and max
		//Throw away first 2 samples and last 2 samples and average the center 12
		for (j=0;j<FLEX_SENSORS_NUM;j++) {
			ave_max[j] = 0;
			ave_min[j] = 0;
			for (i=2;i<(HIST_SIZE-2);i++) {
				ave_max[j] += 2*sensor_max[j][i];
				ave_min[j] += sensor_min[j][i];
			}
			
			ave_max[j] /= (HIST_SIZE-4);
			ave_min[j] /= (HIST_SIZE-4);
			
			cal_max[j] = ave_max[j];
			cal_min[j] = ave_min[j];
		}
		
		cal_flags |= METTIS_CAL_COMPLETE;  //Calibration has been done once
	}
	
}

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
uint8 mettis_get_state( void ) {
	
	//return (cadence_state & 0x0F)<<4 | (cal_steps & 0x0F);
	
	//return (cadence_state & 0x3)<<6 | (sensor_state[MEDIAL] & 0x3)<<4 | (sensor_state[LATERAL] & 0x3)<<2 | (sensor_state[HEEL] & 0x3);
	
	return sensor_ave[MEDIAL]>>12;
	
}


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
uint8 mettis_get_flags( void ) {
	uint8 mettis_flags = 0;
	
	
	
	if (cal_flags == (METTIS_CAL_MIN_COMPLETE | METTIS_CAL_MAX_COMPLETE) ) {
		mettis_flags |= 0x01;
	}
	
	return mettis_flags;
}

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
void mettis_force_cal_max( void ) {
	cal_flags |= METTIS_CAL_FORCE_MAX;
}


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
void mettis_force_cal_min( void ) {
	cal_flags |= METTIS_CAL_FORCE_MIN;
}






/*******************************************************************************
* Function Name: mettis_convert_scaled
********************************************************************************
* Summary:
*        Scales flex sensor values based on calibration min and max.
*        Values at the min will be 0. Values at the max will be 128. Therefore
*        Scaled values can be up to 2x max
*
* Parameters:
*  Array to put scalled sensor values in, Array of unscaled sensor values
*
* Return:
*  None
*

*******************************************************************************/
void mettis_convert_scaled(uint8 scaled[], int unscaled[]) {
	int i;
	int scaling[FLEX_SENSORS_NUM];
	int linear[FLEX_SENSORS_NUM];	
	
	//if ( (cal_flags & METTIS_CAL_COMPLETE) != METTIS_CAL_COMPLETE) {
		
	//	flex_sensors_convert_scaled(scaled, unscaled);
	
	//} else {
	
		for (i=0;i<FLEX_SENSORS_NUM;i++) {
			if (cal_max[i] <= cal_min[i]) {
				
				scaled[i] = 0;
				
			} else {
				//Max is 1/4 of total range so we can get 4x the weight before rail out
				//scaling[i] = ( (unscaled[i] - cal_min[i]) * 64) / (cal_max[i] - cal_min[i]);
				scaling[i] = unscaled[i];
				scaling[i] = scaling[i] - cal_min[i];
				scaling[i] = scaling[i]<<6;
				if (cal_max[i] != cal_min[i]) {
					scaling[i] = scaling[i] / (cal_max[i] - cal_min[i]);
				}
				
				//Linearization
				//linear[i] = (scaling[i] * scaling[i]) * 216 / 65536 + scaling[i] * 87 / 256;
				linear[i] = scaling[i];
				
				
				if (linear[i] < 0)
					linear[i] = 0;
					
				if (linear[i] > 255)
					linear[i] = 255;
				
				scaled[i] = linear[i];
			}
		}
	//}
	
	DBG_PRINT_DEC(scaled[0]);
	DBG_PRINT_TEXT("\r\n");
		
	//Debug
	//scaled[0] = cal_max[0] & 0xFF;
	//scaled[1] = (cal_max[0]>>8) & 0xFF;
	//scaled[2] = (cal_max[0]>>16) & 0xFF;
	
	
}

/*******************************************************************************
* Function Name: mettis_get_cadence
********************************************************************************
* Summary:
*  Returns cadence in steps per minute
*
* Parameters:
*  None
*
* Return:
*  cadence in steps per minute
*

*******************************************************************************/
uint8 mettis_get_cadence( void ) {
	uint32 cadence32;
	uint8 cadence8;
	
	if (cadence_samples == 0) {
		cadence32 = 0;
	} else {
		cadence32 = SAMPLES_PER_SECOND * 60 / cadence_samples;
	}
	
	if (cadence32 > 255) {
		cadence8 = 255;
	} else {
		cadence8 = (uint8)cadence32;
	}
	
	return cadence8;
}

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
uint32 mettis_get_total_steps( void ) {
	return total_steps;
}

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
uint8 mettis_get_contact_time( void ) {
	int contact_time32;
	uint8 contact_time8;
	
	contact_time32 = contact_time_samples * CONTACT_TIME_MS_PER_SAMPLE;
	
	if (contact_time32 > 255) {
		contact_time8 = 255;
	} else {
		contact_time8 = (uint8)contact_time32;
	}
	
	return contact_time8;
}

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
uint8 mettis_get_air_time( void ) {
	int air_time32;
	uint8 air_time8;
	
	air_time32 = air_time_samples * AIR_TIME_MS_PER_SAMPLE;
	
	if (air_time32 > 255) {
		air_time8 = 0;
	} else {
		air_time8 = (uint8)air_time32;
	}
	
	return air_time8;
}

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
uint8 mettis_get_heel2toe( void ) {
	uint8 heel2toe_time;
	
	if (heel2toe_samples > 255) {
		heel2toe_time = 255;
	} else {
		heel2toe_time = (uint8)heel2toe_samples;
	}
	
	return heel2toe_time;
}

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
uint8 mettis_get_impact_force( void ) {
	int i;
	int impact_force = 0;
	
	for (i=0;i<FLEX_SENSORS_NUM;i++) {
		if (cal_max[i] != 0) {
			impact_force += sensor_max[i][0] * IMPACT_FORCE_100_PERCENT / cal_max[i];
		} 
	}
	
	return (uint8) impact_force;
}

/*******************************************************************************
* Function Name: mettis_get_pronation
********************************************************************************
* Summary:
*  Returns the prontaion amount. Up to 100% is overpronation (lots of toe or medial) , -100% is underpronation (lots of pinky or lateral)
*
* Parameters:
*  None
*
* Return:
*  pronation
*

*******************************************************************************/
uint8 mettis_get_pronation( void ) {
	
	int32 medial_force = 0;
	int32 lateral_force = 0;
	int32 pronation = 100;
	
	if (cal_max[MEDIAL] != 0) {
		medial_force = sensor_max[MEDIAL][0] * 256 / cal_max[MEDIAL];
	}
	
	if (cal_max[LATERAL] != 0) {
		lateral_force = sensor_max[LATERAL][0] * 256 / cal_max[LATERAL];
	}
	
	if (medial_force != lateral_force) {
		pronation = medial_force * 200 / (medial_force + lateral_force);
	}
	
	if (pronation >= 100) {
		pronation -= 100;
	} else {
		pronation += 156;
	}
	
	return (uint8) pronation;
	
}

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
uint8 mettis_get_cal_steps( void ) {
	uint8 steps;
	
	if (cal_steps > CADENCE_CAL_STEPS_THRESH) {
		steps = CADENCE_CAL_STEPS_THRESH;
	} else {
		steps = (uint8)cal_steps;
	}
	
	return steps;
}




/* [] END OF FILE */
