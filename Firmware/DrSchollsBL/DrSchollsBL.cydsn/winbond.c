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

#include <my_spi.h>
#include <winbond.h>
#include "debug.h"


int winbond_read_id( void ) {
	//Tests whether there is a winbond chip listening
	uint8 buffer[16];
	
	buffer[0] = 0x90; //Read manufacturer ID
	buffer[1] = 0;
	buffer[2] = 0;
	buffer[3] = 0;
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 6);
	
	F_SS_Write(1);
	
	if (buffer[4] != 0xEF) {
		return 0;
	} else {
		return buffer[5];
	}	
	
}


 void winbond_test( void ) {
	uint8 beforeErase[256];
	uint8 afterWrite[256];
	uint8 buffer[256];
	int i;
	int status;
	
	status = winbond_read_id();
	if (status == 0) {
		DBG_PRINT_TEXT("Invalid Flash Manufacturer ID FAILED\r\n");
		return;
	} else {
		DBG_PRINTF("Flash device ID = %x\r\n", status);
	}
	
	status = 1;
	//Performs erase, read, write test
	
	#define START_ADDR 0
	#define LEN 128
	
	winbond_ota_read_buf(START_ADDR, beforeErase, LEN);
	
	for (i=0;i<256;i++) {
		buffer[i] = i;
	}
			
	winbond_ota_write_buf(START_ADDR, buffer, LEN);
	
	winbond_ota_read_buf(START_ADDR, afterWrite, LEN);
	
	//Compare
	for (i=0;i<LEN;i++) {
		if (buffer[i] != afterWrite[i]) {
			status = 0;
		}
	}
	
	if (status == 0) {
		DBG_PRINT_TEXT("Flash test FAILED\r\n");
		DBG_PRINT_TEXT("beforeErase:\r\n");
	    DBG_PRINT_ARRAY(beforeErase, 256);
	    DBG_PRINT_TEXT("\r\n");
		
		DBG_PRINT_TEXT("buffer:\r\n");
	    DBG_PRINT_ARRAY(buffer, 256);
	    DBG_PRINT_TEXT("\r\n");
		
		DBG_PRINT_TEXT("afterWrite:\r\n");
	    DBG_PRINT_ARRAY(afterWrite, 256);
	    DBG_PRINT_TEXT("\r\n");
	} else {
		DBG_PRINT_TEXT("Flash test PASSED\r\n");
	}
}



void winbond_write_buf(uint32 dataAddr, uint8 *data, uint32 dataSize) {
	uint8 buffer[16];
	
	winbond_write_enable();
			
	buffer[0] = 0x02;	//Page program
	buffer[1] = (dataAddr>>16) & 0xFF;
	buffer[2] = (dataAddr>>8) & 0xFF;
	buffer[3] = (dataAddr) & 0xFF;
	
	F_SS_Write(0);
		
	SPI_exchange(buffer, 4);
	
	//dataSize must be less than a page
	
	SPI_exchange_delay(data, dataSize);  //Program
	
	F_SS_Write(0);
	
}

void winbond_ota_write_buf(uint32 dataAddr, uint8 *data, uint32 dataSize) {
	
	uint32 pageAddr = dataAddr & ~(WINBOND_PAGE_SIZE-1);
	
	if (dataSize != WINBOND_PAGE_SIZE) {
		
		DBG_PRINTF("datasize %lx != WINBOND_PAGE_SIZE\r\n", dataSize);
		
		return;
	}
	
	winbond_ota_program_page(pageAddr, 0, data, dataSize);
	
	
}

void winbond_ota_program_page(uint32 pageAddr, uint32 data_ptr, uint8 *data, uint32 dataSizeLeft) {
	uint8 buffer[16];
	uint8 status;
	
		
	if (pageAddr == 0u) {
		DBG_PRINT_TEXT("*** winbond_program_page(");
		DBG_PRINT_HEX( (uint16) pageAddr);
		DBG_PRINT_TEXT(", ");
		DBG_PRINT_HEX( (uint16) data_ptr);
		DBG_PRINT_TEXT(", ");
		DBG_PRINT_HEX( (uint16) dataSizeLeft);
		DBG_PRINT_TEXT(") ***\r\n");
		DBG_PRINT_ARRAY(data, dataSizeLeft);
		DBG_PRINT_TEXT("\r\n");
	}
	
	if (pageAddr == 0u) {
		uint8 check[128];
		winbond_ota_read_buf(0, check, 128);
		DBG_PRINTF("Before erase:\r\n");
		DBG_PRINT_ARRAY(check, 128);
		DBG_PRINTF("\r\n");
	}
	
	winbond_page_erase(pageAddr);  //Erase the page
	
	
	if (pageAddr == 0u) {
		uint8 check[128];
		winbond_ota_read_buf(0, check, 128);
		DBG_PRINTF("After erase:\r\n");
		DBG_PRINT_ARRAY(check, 128);
		DBG_PRINTF("\r\n");
	}
	
	winbond_write_enable();
			
	buffer[0] = 0x02;	//Page program
	buffer[1] = (pageAddr>>11) & 0xFF;
	buffer[2] = (pageAddr>>3) & 0xFF;  //Skipping bits 11-7 to make sectors the same as the PSoC page size
	buffer[3] = 0;
	
	F_SS_Write(0);
		
	SPI_exchange(buffer, 4);
		
	SPI_exchange_delay(data, dataSizeLeft);  //Program entire page
	
	F_SS_Write(0);
	
	while (1) {
		CyDelay(1);
		
		status = winbond_read_status();
		if ( (status & WINBOND_STATUS_BUSY) != WINBOND_STATUS_BUSY) {
			break;
		}
	}

	return;
	
	
}

void winbond_ota_read_page(uint32 pageAddr, uint32 data_ptr, uint8 *data, uint32 dataSizeLeft) {
	uint8 buffer[16];
	
	buffer[0] = 0x0B; //Fast read
	buffer[1] = (pageAddr>>11) & 0xFF;
	buffer[2] = (pageAddr>>3) & 0xFF;  //Skipping bits 11-7 to make sectors the same as the PSoC page size
	buffer[3] = 0;
	buffer[4] = 0; //dummy byte
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 5);
	
	SPI_exchange(data, dataSizeLeft);
	
	F_SS_Write(1);
	
		
	if (pageAddr == 0u) {
		DBG_PRINT_TEXT("*** winbond_ota_read_page(");
		DBG_PRINT_HEX( (uint16) pageAddr);
		DBG_PRINT_TEXT(", ");
		DBG_PRINT_HEX( (uint16) data_ptr);
		DBG_PRINT_TEXT(", ");
		DBG_PRINT_HEX( (uint16) dataSizeLeft);
		DBG_PRINT_TEXT(") ***\r\n");
		DBG_PRINT_ARRAY(data, dataSizeLeft);
		DBG_PRINT_TEXT("\r\n");
	}
		
}


void winbond_ota_read_buf(uint32 dataAddr, uint8 *data, uint32 dataSize) {
		
	//DBG_PRINT_TEXT("*** winbond_ota_read_buf(");
	//DBG_PRINT_HEX( (uint16) dataAddr);
	//DBG_PRINT_TEXT(", , ");
	//DBG_PRINT_HEX( (uint16) dataSize);
	//DBG_PRINT_TEXT(") ***\r\n");
	
	if (dataSize == 0) {
		return;
	}
	
	winbond_ota_read_page(dataAddr, 0, data, dataSize);
		
			
}

void winbond_fast_read(uint32 dataAddr, uint8 *data, uint32 dataSize) {
	uint8 buffer[16];
	
	buffer[0] = 0x0B; //Fast read
	buffer[1] = (dataAddr>>16) & 0xFF;
	buffer[2] = (dataAddr>>8) & 0xFF;
	buffer[3] = dataAddr & 0xFF;
	buffer[4] = 0; //dummy byte
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 5);
	
	SPI_exchange(data, dataSize);
	
	F_SS_Write(1);

}


void winbond_page_erase(uint32 pageAddr) {
	uint8 buffer[16];
	uint8 status;
	

	winbond_write_enable();
	
	//Execute sector erase
	buffer[0] = 0x20;
	buffer[1] = (pageAddr>>11) & 0xFF;
	buffer[2] = (pageAddr>>3) & 0xFF;  //Skipping bits 11-7 to make sectors the same as the PSoC page size
	buffer[3] = 0;
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 4);
	
	F_SS_Write(1);
	
	while (1) {
		CyDelay(10);
		
		status = winbond_read_status();
		if ( (status & WINBOND_STATUS_BUSY) != WINBOND_STATUS_BUSY) {
			break;
		}
	}

	
}

void winbond_block_erase_64(uint32 blockAddr) {
	uint8 buffer[16];
	uint8 status;
	

	winbond_write_enable();
	
	//Execute block erase 64K size
	buffer[0] = 0xD8;
	buffer[1] = (blockAddr>>11) & 0xFF;
	buffer[2] = (blockAddr>>3) & 0xFF;  //Skipping bits 11-7 to make sectors the same as the PSoC page size
	buffer[3] = 0;
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 4);
	
	F_SS_Write(1);
	
	while (1) {
		CyDelay(10);
		
		status = winbond_read_status();
		if ( (status & WINBOND_STATUS_BUSY) != WINBOND_STATUS_BUSY) {
			break;
		}
	}

	
}

void winbond_start_block_erase_64(uint32 blockAddr) {
	uint8 buffer[16];
	

	winbond_write_enable();
	
	//Execute block erase 64K size
	buffer[0] = 0xD8;
	buffer[1] = (blockAddr>>16) & 0xFF;
	buffer[2] = (blockAddr>>8) & 0xFF; 
	buffer[3] = (blockAddr) & 0xFF;
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 4);
	
	F_SS_Write(1);
	
		
}


void winbond_chip_erase( void ) { 
	uint8 buffer[16];
	uint8 status;
	
	while (1) {
	
		winbond_write_enable();
		
		//Execute chip erase
		buffer[0] = 0xC7;
		
		F_SS_Write(0);
		
		SPI_exchange(buffer, 1);
		
		F_SS_Write(1);
		
		CyDelay(1);
		
		status = winbond_read_status();
		if ( (status & WINBOND_STATUS_BUSY) != WINBOND_STATUS_BUSY) {
			continue;
		}		
	
		while (1) {
			CyDelay(1000);
			
			status = winbond_read_status();
			if ( (status & WINBOND_STATUS_BUSY) != WINBOND_STATUS_BUSY) {
				break;
			}
		}
	
		break;
	}
}

void winbond_start_erase( void ) { 
	uint8 buffer[16];
	
	//Non-blocking
	winbond_write_enable();
	
	//Execute chip erase
	buffer[0] = 0xC7;
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 1);
	
	F_SS_Write(1);
		
}


void winbond_write_enable( void ) {
	uint8 buffer[16];
	
	buffer[0] = 0x06;
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 1);
	
	F_SS_Write(1);
	
	
}

uint8 winbond_read_status( void ) {
	uint8 buffer[16];
	
	buffer[0] = 0x05;
	buffer[1] = 0xFF;
	
	F_SS_Write(0);
	
	SPI_exchange(buffer, 2);
	
	F_SS_Write(1);
	
	return buffer[1];
	
}




/* [] END OF FILE */
