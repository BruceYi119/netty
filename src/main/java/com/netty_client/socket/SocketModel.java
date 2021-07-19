package com.netty_client.socket;

import java.io.File;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SocketModel {

	private int fileSize;
	private int sendSize;
	private int maxDataSize = 5085;
	@NonNull
	private File file;
	private byte[] data;
	private boolean send = false;
	private ByteBuf packet;
	private StringBuffer sb;

}