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

#include <EMI_SPIM_SPI_UART.h>

void SPI_exchange(uint8 buffer[], uint32 count) {
	
	uint32 i;
	uint8 data;
	
	if (count > 0) {
	
		EMI_SPIM_SpiUartClearTxBuffer();
		EMI_SPIM_SpiUartClearRxBuffer();
		
		EMI_SPIM_ClearMasterInterruptSource(EMI_SPIM_INTR_MASTER_SPI_DONE);
		
		for (i=0;i<count;i++) {
			data = buffer[i];	            
	    	EMI_SPIM_SpiUartPutArray(&data, 1);
	                                  
		    while(0u == (EMI_SPIM_GetMasterInterruptSource() & EMI_SPIM_INTR_MASTER_SPI_DONE))
		    {
		        /* Wait while Master completes transaction */
		    }
			
			EMI_SPIM_ClearMasterInterruptSource(EMI_SPIM_INTR_MASTER_SPI_DONE);
			
			buffer[i] = EMI_SPIM_SpiUartReadRxData();
			
		}
	       
	}
	
}

void SPI_exchange_delay(uint8 buffer[], uint32 count) {
	
	uint32 i;
	uint8 data;
	
	if (count > 0) {
	
		EMI_SPIM_SpiUartClearTxBuffer();
		EMI_SPIM_SpiUartClearRxBuffer();
		
		EMI_SPIM_ClearMasterInterruptSource(EMI_SPIM_INTR_MASTER_SPI_DONE);
		
		for (i=0;i<count;i++) {
			data = buffer[i];	            
	    	EMI_SPIM_SpiUartPutArray(&data, 1);
	                                  
		    while(0u == (EMI_SPIM_GetMasterInterruptSource() & EMI_SPIM_INTR_MASTER_SPI_DONE))
		    {
		        /* Wait while Master completes transaction */
		    }
			
			EMI_SPIM_ClearMasterInterruptSource(EMI_SPIM_INTR_MASTER_SPI_DONE);
			
			buffer[i] = EMI_SPIM_SpiUartReadRxData();
			
			if (i == 0) {
				CyDelayUs(50);
			} else {
				CyDelayUs(10);
			}
		}
	       
	}
	
}



/* [] END OF FILE */
