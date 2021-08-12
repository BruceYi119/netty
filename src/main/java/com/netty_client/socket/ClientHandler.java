package com.netty_client.socket;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netty_client.component.Telegram;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

public class ClientHandler extends ChannelInboundHandlerAdapter {

	public static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private SocketModel model;
	private int recvMsgSize = 12;

	public ClientHandler(String fileName, String filePath, int fileSize) {
		initModel(fileName, filePath, fileSize);
	}

	private void initModel(String fileName, String filePath, int fileSize) {
		model = new SocketModel();
		model.setFileName(fileName);
		model.setFilePath(filePath);
		model.setFileSize(fileSize);
		model.setSb(new StringBuffer());
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().pipeline().addLast(new ReadTimeoutHandler(30));
		ctx.channel().pipeline().addLast(new WriteTimeoutHandler(30));

		model.setPacket(ctx.alloc().buffer());
		model.setFileBuf(ctx.alloc().buffer());

		byte[] sendBytes = getTelegram("I", 35);
		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(sendBytes);

		ctx.writeAndFlush(bb);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf b = (ByteBuf) msg;

		if (model.getPacket() == null)
			model.setPacket(ctx.alloc().buffer());

		model.getPacket().writeBytes(b);
		b.release();

		process(ctx);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		clearModel();

		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		clearModel();

		log.error("exceptionCaught : ", cause);

		ctx.close();
	}

	private void process(ChannelHandlerContext ctx) {
		while (model.getPacket().readableBytes() >= recvMsgSize) {
			byte[] sendBytes = null;
			byte[] recvBytes = new byte[recvMsgSize];

			model.getPacket().readBytes(recvBytes, 0, recvBytes.length);

			switch ((char) recvBytes[0]) {
			// 전송
			case 'S':
				// 최초 전송시
				if (!model.isSend()) {
					log.info(String.format("ch-%s : 최초 전송시", ctx.channel().id()));
					RandomAccessFile raf = null;
					try {
						raf = new RandomAccessFile(model.getFilePath(), "r");
						char sendType = (char) recvBytes[1];
						byte[] data = new byte[model.getFileSize()];
						raf.read(data);
						raf.close();
						model.getFileBuf().writeBytes(data);

						if (sendType == 'I') {
							byte[] sendSizeByte = new byte[10];

							System.arraycopy(recvBytes, 2, sendSizeByte, 0, sendSizeByte.length);

							int sendSize = Integer.parseInt(new String(sendSizeByte));

							model.setSendSize(sendSize);
							model.getFileBuf().readBytes(sendSize);
						}
					} catch (IOException e) {
						log.error("IOException : ", e);
					}

					model.setSend(true);
				}

				// 전송 전문
				if (model.getFileSize() > model.getSendSize()) {
					int sendSize = (model.getFileSize() - model.getSendSize()) > model.getMaxDataSize()
							? model.getMaxDataSize()
							: model.getFileSize() - model.getSendSize();
					byte[] data = new byte[sendSize];
					model.getFileBuf().readBytes(data).discardReadBytes();

					sendBytes = getTelegram("S", data.length + 35, data);

					model.setSendSize(model.getSendSize() + sendSize);
					// 전송완료 전문
				} else {
					sendBytes = getTelegram("E", 35);
				}

				ByteBuf bb = Unpooled.buffer();
				bb.writeBytes(sendBytes);
				ctx.writeAndFlush(bb);
				break;
			case 'W':
				log.error("ERROR");
				clearModel();
				ctx.close();
				log.info(String.format("ch-%s : 에러", ctx.channel().id()));
				break;
			// 전송완료
			default:
				clearModel();
				ctx.close();
				log.info(String.format("ch-%s : 전송완료", ctx.channel().id()));
				break;
			}

		}
	}

	private byte[] getTelegram(String type, int teleSize) {
		byte[] bytes = new byte[teleSize];

		model.getSb().setLength(0);

		// 전문타입 1 (I : 개시 전문/S : 전송/E : 마지막 전송)
		model.getSb().append(type);
		// 전문길이 4
		model.getSb().append(Telegram.numPad(teleSize, 4));
		// 파일명 20
		model.getSb().append(Telegram.strPad(model.getFileName(), 20));
		// 파일크기 10
		model.getSb().append(Telegram.numPad(model.getFileSize(), 10));

		bytes = model.getSb().toString().getBytes();

		return bytes;
	}

	private byte[] getTelegram(String type, int teleSize, byte[] data) {
		byte[] bytes = new byte[teleSize];

		model.getSb().setLength(0);

		// 전문타입 1 (I : 개시 전문/S : 전송/E : 전송완료)
		model.getSb().append(type);
		// 전문길이 4
		model.getSb().append(Telegram.numPad(teleSize, 4));
		// 파일명 20
		model.getSb().append(Telegram.strPad(model.getFileName(), 20));
		// 파일크기 10
		model.getSb().append(Telegram.numPad(model.getFileSize(), 10));
		System.arraycopy(model.getSb().toString().getBytes(), 0, bytes, 0, 35);
		// 파일내용 36~5120 가변 (max : 5085)
		System.arraycopy(data, 0, bytes, 35, data.length);

		return bytes;
	}

	private void clearModel() {
		if (model.getPacket() != null) {
			model.getPacket().readerIndex(model.getPacket().writerIndex());
			while (model.getPacket().refCnt() > 0)
				model.getPacket().release();
		}
	}

}