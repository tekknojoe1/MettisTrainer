
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

#if !defined(my_spi_H)
#define my_spi_H
	
#include <project.h>

void SPI_exchange(uint8 buffer[], uint32 count);
void SPI_exchange_delay(uint8 buffer[], uint32 count);

#endif /* my_spi_H */

/* [] END OF FILE */
