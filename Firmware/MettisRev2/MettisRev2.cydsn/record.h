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

#if !defined(record_H)
#define record_H

#include <project.h>	
	
	
#define RECORD_DATA_SIZE 8 //8 bytes for each record size	
	
#define RECORD_SAMPLE_SIZE 60000 //60 seconds at 1000 samples each
#define RECORD_MAX_ADDR (RECORD_SAMPLE_SIZE * RECORD_DATA_SIZE)
	

/*******************************************************************************
* Function Name: record_start
********************************************************************************
* Summary:
*  Starts a recording session
*
* Parameters:
*  Mode - 0 = detail, 1 = summarized
*
* Return:
* 	None
*

*******************************************************************************/
void record_start(int mode);

/*******************************************************************************
* Function Name: record_play
********************************************************************************
* Summary:
*  Plays back the recording data
*
* Parameters:
*  None
*
* Return:
* 	None
*

*******************************************************************************/
void record_play( void );

/*******************************************************************************
* Function Name: record_stop
********************************************************************************
* Summary:
*  Stops recording or playing back
*
* Parameters:
*  None
*
* Return:
* 	None
*

*******************************************************************************/
void record_stop( void );

/*******************************************************************************
* Function Name: record_erase
********************************************************************************
* Summary:
*  Checks if flash is erased, then starts an erase if not erased
*
* Parameters:
*  None
*
* Return:
* 	None
*

*******************************************************************************/
void record_erase( void );

/*******************************************************************************
* Function Name: record_isBusy
********************************************************************************
* Summary:
*  Returns whether the erase or write functions are still in progress
*
* Parameters:
*  None
*
* Return:
* 	Integer 0 for done and 1 for busy
*

*******************************************************************************/
int record_isBusy( void );

/*******************************************************************************
* Function Name: record_isRecording
********************************************************************************
* Summary:
*  Returns whether recording is in progress
*
* Parameters:
*  None
*
* Return:
* 	Integer 0 for not recording and 1 for recording
*

*******************************************************************************/
int record_isRecording( void );

/*******************************************************************************
* Function Name: record_isPlayback
********************************************************************************
* Summary:
*  Returns whether playback is in progress
*
* Parameters:
*  None
*
* Return:
* 	Integer 0 for not playing and 1 for playing
*

*******************************************************************************/
int record_isPlayback( void );

/*******************************************************************************
* Function Name: record_play_next
********************************************************************************
* Summary:
*  Retrieves next play sample
*
* Parameters:
*  buffer to place play sample in
*
* Return:
* 	None
*

*******************************************************************************/
void record_play_next(uint8 play_data[] );


/*******************************************************************************
* Function Name: record_post_data
********************************************************************************
* Summary:
*  Posts sensor data already formated to be written to flash
*
* Parameters:
*  None
*
* Return:
* 	None
*

*******************************************************************************/
void record_post_data(uint8 post_data[] );

/*******************************************************************************
* Function Name: record_write_data
********************************************************************************
* Summary:
*  Writes sensor data already posted to flash
*
* Parameters:
*  None
*
* Return:
* 	None
*

*******************************************************************************/
void record_write_data( void );

/*******************************************************************************
* Function Name: record_task
********************************************************************************
* Summary:
*  Takes care of record state machine
*
* Parameters:
*  None
*
* Return:
* 	None
*

*******************************************************************************/
void record_task( void );

/*******************************************************************************
* Function Name: record_get_flags
********************************************************************************
* Summary:
*  Returns the record_flags
*
* Parameters:
*  None
*
* Return:
* 	record_flags
*

*******************************************************************************/
uint8 record_get_flags( void );


#endif /* record_H */
/* [] END OF FILE */
