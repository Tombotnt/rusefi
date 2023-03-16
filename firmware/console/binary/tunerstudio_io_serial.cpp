/**
 * Implementation for hardware-serial TunerStudio ports
 */

#include "tunerstudio_io.h"

#if defined(TS_PRIMARY_UxART_PORT) || defined(TS_SECONDARY_UxART_PORT)
#if HAL_USE_SERIAL
void SerialTsChannel::start(uint32_t baud) {
	SerialConfig cfg = {
		#if EFI_PROD_CODE
			.speed = baud,
			.cr1 = 0,
			.cr2 = USART_CR2_STOP1_BITS | USART_CR2_LINEN,
			.cr3 = 0
		#endif // EFI_PROD_CODE
	};

	sdStart(m_driver, &cfg);
}

void SerialTsChannel::stop() {
	sdStop(m_driver);
}

void SerialTsChannel::write(const uint8_t* buffer, size_t size, bool /*isEndOfPacket*/) {
	size_t transferred = chnWriteTimeout(m_driver, buffer, size, BINARY_IO_TIMEOUT);
	bytesOut += transferred;
}

size_t SerialTsChannel::readTimeout(uint8_t* buffer, size_t size, int timeout) {
	size_t transferred = chnReadTimeout(m_driver, buffer, size, timeout);
	bytesIn += transferred;
    return transferred;
}

#endif // HAL_USE_SERIAL

#if HAL_USE_UART

#ifndef USART_CR2_STOP1_BITS
// todo: acticulate why exactly does prometheus_469 as for this hack
#define USART_CR2_STOP1_BITS 0
#endif

void UartTsChannel::start(uint32_t baud) {
	m_config = { 
		.txend1_cb 		= NULL,
		.txend2_cb 		= NULL,
		.rxend_cb 		= NULL,
		.rxchar_cb		= NULL,
		.rxerr_cb		= NULL,
		.timeout_cb		= NULL,
		.speed 			= baud,
		.cr1 			= 0,
		.cr2 			= USART_CR2_STOP1_BITS | USART_CR2_LINEN,
		.cr3 			= 0,
		.rxhalf_cb		= NULL
	};

	uartStart(m_driver, &m_config);
}

void UartTsChannel::stop() {
	uartStop(m_driver);
}

void UartTsChannel::write(const uint8_t* buffer, size_t size, bool) {
	size_t transferred = uartSendTimeout(m_driver, &size, buffer, BINARY_IO_TIMEOUT);
	bytesOut += transferred;
}

size_t UartTsChannel::readTimeout(uint8_t* buffer, size_t size, int timeout) {
	// nasty in/out parameter approach:
	// in entry: number of data frames to receive
	// on exit the number of frames actually received
	uartReceiveTimeout(m_driver, &size, buffer, timeout);
	bytesIn += size;
	return size;
}
#endif // HAL_USE_UART
#endif // defined(TS_PRIMARY_UxART_PORT) || defined(TS_SECONDARY_UxART_PORT)
