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

#if !defined(winbond_H)
#define winbond_H

#include <project.h>
	
	
#define WINBOND_STATUS_BUSY 0x01
	
#define WINBOND_PAGE_SIZE 128 //Matches flash page size in PSoC
#define WINBOND_SECTOR_SIZE 4096
#define WINBOND_BLOCK_SIZE 65536

#define WINBOND_FLASH_SIZE ( (64/8)*1024*1024) //64Mbit
	
	
void winbond_test( void );
int winbond_read_id( void );
void winbond_write_buf(uint32 dataAddr, uint8 *data, uint32 dataSize);
void winbond_ota_write_buf(uint32 dataAddr, uint8 *data, uint32 dataSize);
void winbond_ota_program_page(uint32 pageAddr, uint32 data_ptr, uint8 *data, uint32 dataSizeLeft);
void winbond_ota_read_page(uint32 pageAddr, uint32 data_ptr, uint8 *data, uint32 dataSizeLeft);
void winbond_ota_read_buf(uint32 dataAddr, uint8 *data, uint32 dataSize);
void winbond_fast_read(uint32 dataAddr, uint8 *data, uint32 dataSize);
void winbond_page_erase(uint32 pageAddr);
void winbond_block_erase_64(uint32 blockAddr);
void winbond_start_block_erase_64(uint32 blockAddr);
void winbond_chip_erase( void );
void winbond_start_erase( void );
void winbond_write_enable( void );
uint8 winbond_read_status ( void );

#endif /* winbond_H */
/* [] END OF FILE */
