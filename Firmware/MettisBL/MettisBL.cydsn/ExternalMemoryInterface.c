/*******************************************************************************
* File Name: ExternalMemoryInterface.c
*
* Version: 1.0
*
* Description:
*  Provides an API for the external memory access.
*
* Hardware Dependency:
*  CY8CKIT-042 BLE
*
********************************************************************************
* Copyright 2015, Cypress Semiconductor Corporation. All rights reserved.
* This software is owned by Cypress Semiconductor Corporation and is protected
* by and subject to worldwide patent and copyright laws and treaties.
* Therefore, you may use this software only as provided in the license agreement
* accompanying the software package from which you obtained this software.
* CYPRESS AND ITS SUPPLIERS MAKE NO WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* WITH REGARD TO THIS SOFTWARE, INCLUDING, BUT NOT LIMITED TO, NONINFRINGEMENT,
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
*******************************************************************************/

#include "Options.h"
#include "ExternalMemoryInterface.h"
#include "cytypes.h"
#include "debug.h"
#include <project.h>
#include <winbond.h>


uint8 emiWriteBuffer[EMI_SIZE_OF_WRITE_BUFFER];



/*******************************************************************************
* Function Name: EMI_Start
********************************************************************************
*
* Summary:
*  Starts the external memory interface.
*
* Parameters:
*  None
*
* Return:
*  None
*******************************************************************************/
void EMI_Start(void)
{
    EMI_SPIM_Start();
}


/*******************************************************************************
* Function Name: EMI_SetPointer
********************************************************************************
*
* Summary:
*  Sets the internal pointer of the external memory.
*
* Parameters:
*  uint32 dataAddr:
*   The internal pointer value.
*
* Return:
*  Status
*     Value               Description
*    CYRET_SUCCESS           Successful
*    Other non-zero                 Failure
*******************************************************************************/
cystatus EMI_SetPointer(uint32 dataAddr)
{
    uint32 status = CYRET_UNKNOWN;
    status =  EMI_WriteData(dataAddr, EMI_NO_DATA_SIZE, NULL);
    return (status);
}

/*******************************************************************************
* Function Name: EMI_WriteData
********************************************************************************
*
* Summary:
*  Write data to the external memory.
*
* Parameters:
*  uint32 dataAddr:
*   The internal pointer value.
*  uint32 dataSize:
*   Size of input data
*  uint8 *data:
*   Pointer to data that is written to external memory
*
* Return:
*  Status
*     Value               Description
*    CYRET_SUCCESS           Successful
*    Other non-zero          Failure
*******************************************************************************/
cystatus EMI_WriteData(uint32 dataAddr, uint32 dataSize, uint8 *data)
{
    
    #if (ENCRYPTION_ENABLED == YES)
        if (dataAddr >= (META_DATA_ADDR + META_DATA_SIZE) && (dataSize>0))
        {
            uint8 key[KEY_LENGTH] = {0};
            uint8 nonce[NONCE_LENGTH] = {0};
            uint8 encrypted[ENC_BUFFER_SIZE] = {0};
            uint8 out_mic[MIC_DATA_LENGTH];
            CYBLE_API_RESULT_T result;
            
            CR_ReadKey(key);
            CR_ReadNonce(nonce);
            result = CR_Encrypt(data, dataSize, key, nonce, encrypted, out_mic);
            
            if (result == CYBLE_ERROR_OK)
            {
                memcpy(data, encrypted, dataSize);
            }
            else
            {
                if (result == CYBLE_ERROR_INVALID_PARAMETER)
                {
                    DBG_PRINT_TEXT("===============================================================================\r\n");                    
                    DBG_PRINT_TEXT("=              ENCRYPTION ERROR: CYBLE_ERROR_INVALID_PARAMETER                 \r\n");
                    DBG_PRINT_TEXT("===============================================================================\r\n");                    
                }
                else 
                {
                    DBG_PRINT_TEXT("===============================================================================\r\n");
                    DBG_PRINT_TEXT("=              ENCRYPTION ERROR:UNKNOWN:  ");
                    DBG_PRINT_HEX(result);
                    DBG_PRINT_TEXT("=\r\n");
                    DBG_PRINT_TEXT("===============================================================================\r\n");                    
                }
                return (result);
            }
            
        }
    #endif /* (ENCRYPTION_ENABLED == YES) */
    
	winbond_write_buf(dataAddr, data, dataSize);
	
    return CYRET_SUCCESS;
}


/*******************************************************************************
* Function Name: EMI_ReadData
********************************************************************************
*
* Summary:
*  Read data from the external memory.
*
* Parameters:
*  uint32 dataAddr: The internal pointer value.
*   
*  uint32 dataSize: Size of output data
*   
*  uint8 *data:     Pointer to data that is read from external memory
*   
*
* Return:
*  Status
*     Value               Description
*    CYRET_SUCCESS           Successful
*    Other non-zero          Failure
*    CYBLE_ERROR_INVALID_PARAMETER - problems with decryption
*******************************************************************************/
cystatus EMI_ReadData(uint32 dataAddr, uint32 dataSize, uint8 *data)
{
    cystatus status;
    
    status =  EMI_SetPointer(dataAddr);

    if (CYRET_SUCCESS == status)
    {
        
		winbond_ota_read_buf(dataAddr, data, dataSize);
		
        #if (ENCRYPTION_ENABLED == YES)
        /*Encrypt data except to metadata*/
        

        if (dataAddr >= (META_DATA_ADDR + META_DATA_SIZE) && (dataSize>0))
        {
            uint8 key[KEY_LENGTH] = {0};
            uint8 nonce[NONCE_LENGTH] = {0};
            uint8 decrypted[ENC_BUFFER_SIZE] = {0};
            uint8 out_mic[MIC_DATA_LENGTH]={0};
            CYBLE_API_RESULT_T result;
            
            CR_ReadKey(key);
            CR_ReadNonce(nonce);
            result = CR_Decrypt(data, dataSize, key, nonce, decrypted, out_mic);
            /* Invalid MIC_AUTH not checked  as it will consume additional memory and 
               was not required.*/
            if (result == CYBLE_ERROR_INVALID_PARAMETER)
            {
                DBG_PRINT_TEXT("DECRYPTION ERROR: CYBLE_ERROR_INVALID_PARAMETER            \r\n");
                status = CYBLE_ERROR_INVALID_PARAMETER;
            }
            else
            {
                memcpy(data, decrypted, dataSize);
            }                                            
        }
    #endif /* (ENCRYPTION_ENABLED == YES) */
    }

    return (status);
}

/*******************************************************************************
* Function Name: EMI_EraseAll
********************************************************************************
*
* Summary:
*  Erases content of the external memory.
*
* Parameters:
*  uint16 rowNum:
*   The flash row number. The number of the flash rows is defined by the
*   CY_FLASH_NUMBER_ROWS macro for the selected device. Refer to the device
*   datasheet for the details.
*
*  uint8* rowData:
*   Array of bytes to write. The size of the array must be equal to the flash
*   row size. The flash row size for the selected device is defined by
*   the CY_FLASH_SIZEOF_ROW macro. Refer to the device datasheet for the
*   details.
*
* Return:
*  Status
*     Value                     Description
*    CY_SYS_FLASH_SUCCESS           Successful
*    CY_SYS_FLASH_INVALID_ADDR      Specified flash row address is invalid
*    CY_SYS_FLASH_PROTECTED         Specified flash row is protected
*    Other non-zero                 Failure
*
* Side Effects:
*   The IMO must be enabled before calling this function. The operation of the
*   flash writing hardware is dependent on the IMO.
*
*   For PSoC 4000 devices this API will automatically modify the clock settings
*   for the device. Writing to flash requires that changes be made to the IMO
*   and HFCLK settings. The configuration is restored before returning. HFCLK
*   will have several frequency changes during the operation of this API between
*   a minimum frequency of the current IMO frequency divided by 8 and a maximum
*   frequency of 12 MHz. This will impact the operation of most of the hardware
*   in the device.
*
*******************************************************************************/
cystatus EMI_EraseAll(void)
{
    
	winbond_chip_erase();	
	    
    return CY_SYS_FLASH_SUCCESS;    
}


/* [] END OF FILE */
