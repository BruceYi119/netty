package com.netty_client.socket;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SocketModel {

	private int fileSize;
	private int sendSize;
	private int maxDataSize = 5085;
	private String fileName;
	private String filePath;
	private ByteBuf fileBuf;
	private boolean send = false;
	private ByteBuf packet;
	private StringBuffer sb;

}