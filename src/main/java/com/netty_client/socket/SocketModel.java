package com.netty_client.socket;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SocketModel {

	@NonNull
	private File file;
	private int size;
	private int sendSize;
	private ByteBuf packet;
	private StringBuffer sb;
	private FileInputStream fis;
	private BufferedInputStream bis;

}