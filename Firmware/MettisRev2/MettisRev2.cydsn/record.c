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

#include "record.h"
#include "winbond.h"
#include "mettis.h"
#include "debug.h"

#define RECORD_IDLE  0x00
#define RECORD_START 0x01
#define RECORD_ERASE 0x02
#define RECORDING    0x10
#define RECORD_PLAY  0x80

static uint8 record_state = RECORD_IDLE;

static uint32 record_sample;   //Increments with every sample
static uint32 record_addr;		//Increments with every write
static uint32 play_addr;		//Increments with every read

static uint8 record_data[RECORD_DATA_SIZE];

extern uint8 bootloadingMode;

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
void record_erase( void ) {
	uint8 buffer[8];
	int i;
	int erased = 1;
	
	//Read first 8 bytes of flash
	winbond_fast_read(0, buffer, 8);
	
	//Check if buffer is all 0xFF
	for (i=0;i<8;i++) {
		if (buffer[i] != 0xFF) {
			erased = 0;
		}
	}
	
	//Read last 8 bytes of flash
	winbond_fast_read(WINBOND_FLASH_SIZE-8, buffer, 8);
	
	//Check if buffer is all 0xFF
	for (i=0;i<8;i++) {
		if (buffer[i] != 0xFF) {
			erased = 0;
		}
	}

	if (erased == 0) {
		winbond_start_erase();		//Start erase cycle
	}
	
}

/*******************************************************************************
* Function Name: record_erase_block
********************************************************************************
* Summary:
*  Erases a block of flash
*
* Parameters:
*  Starting address of block to erase
*
* Return:
* 	None
*

*******************************************************************************/
void record_erase_block(uint32 addr ) {

	winbond_start_block_erase_64(addr);
	
}

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
void record_start(int mode) {
	
	if (mode == 0u) {
	
		record_state = RECORD_START;  //Drop whatever we are doing and start a record session
		
	}
	
}

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
void record_play( void ) {
	
	record_state = RECORD_PLAY;
	
	play_addr = 0;
	
	DBG_PRINT_TEXT("\r\nPlaying data...\r\n");
}

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
void record_stop( void ) {
	
	record_state = RECORD_IDLE;
	
	DBG_PRINT_TEXT("\r\nRecieved Stop.\r\n");
}




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
int record_isBusy( void ) {
	uint8 status;
	
	status = winbond_read_status();
	//DBG_PRINTF(":%x", status);
	if ( (status & WINBOND_STATUS_BUSY) == WINBOND_STATUS_BUSY) {
		return 1;
	} else {
		return 0;
	}
}

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
int record_isRecording( void ) {
	return (record_state == RECORDING);
}

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
int record_isPlayback( void ) {
	return (record_state == RECORD_PLAY);
}

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
void record_play_next(uint8 play_data[] ) {
	
	winbond_fast_read(play_addr, play_data, RECORD_DATA_SIZE);
	
	play_addr += RECORD_DATA_SIZE;
	
	if ( (play_addr >= WINBOND_FLASH_SIZE) || (play_addr >= record_addr) ) {
		
		record_state = RECORD_IDLE;
		DBG_PRINT_TEXT("\r\nPlay done.\r\n");
	}
	
}

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
void record_post_data(uint8 post_data[] ) {
	int i;
	
	if (record_state == RECORDING) {
		
		for (i=0;i<RECORD_DATA_SIZE;i++) {
			record_data[i] = post_data[i];
		}
		
		record_data[6] = (record_sample>>8) & 0x7F;  //Leave top bit cleared so a record can't be all 0xFFs
		record_data[7] = (record_sample) & 0xFF;
		
		record_sample++;
	}
}


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
void record_write_data( void ) {
	
	CyDelay(1);
	winbond_write_buf(record_addr, record_data, RECORD_DATA_SIZE);
	
}

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
void record_task( void ) {
	
	if (bootloadingMode != 0u) {
		record_state = RECORD_IDLE;  //Do not allow recording to take place when doing OTA
	}
	
	
	switch (record_state) {
	
		case RECORD_START: 
		
			DBG_PRINT_TEXT("Record started...\r\n");
		
			//Reset pointers
			record_sample = 0;
			record_addr = 0;
		
			record_state = RECORD_ERASE;
		
			DBG_PRINTF("Erasing Block:%X\r\n", (uint16)record_addr);
			
			record_erase_block(record_addr);
			
		break;
		
		case RECORD_ERASE:
		
			if (record_isBusy() ) {
				break;
			}
		
			record_addr += WINBOND_BLOCK_SIZE;
			
			if (record_addr >= RECORD_MAX_ADDR) {
				
				record_addr = 0;
				record_state = RECORDING;
			
			} else {
			
				DBG_PRINTF("Erasing Block:%X\r\n", (uint16)(record_addr>>16) );
				
				record_erase_block(record_addr);
			
			}
		
		break;
		
		case RECORDING:
			
			if (record_isBusy() ) {
				break;
			}
			
			//Write record data
			record_write_data();
			
			record_addr += RECORD_DATA_SIZE;
			
			if (record_addr >= RECORD_MAX_ADDR) {
				
				DBG_PRINT_TEXT("\r\nRecord done.\r\n");
				record_state = RECORD_IDLE;
			}
			
		break;
			
		case RECORD_PLAY:
						
		break;
		
		default: //RECORD_IDLE
		break;
	}
}


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
uint8 record_get_flags( void ) {
	
	return (record_state & 0xF0);

}

/* [] END OF FILE */
