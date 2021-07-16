package com.netty_client.socket;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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

	public ClientHandler(File file) {
		this.model = new SocketModel(file);

		initModel();
	}

	private void initModel() {
		model.setSize((int) model.getFile().length());
		model.setSb(new StringBuffer());
		try {
			model.setFis(new FileInputStream(model.getFile()));
			model.setBis(new BufferedInputStream(model.getFis(), (int) model.getFile().length()));
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException : ", e);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().pipeline().addLast(new ReadTimeoutHandler(30));
		ctx.channel().pipeline().addLast(new WriteTimeoutHandler(30));

		model.setPacket(ctx.alloc().buffer());
		model.getSb().setLength(0);

		// 전문타입 1 (I : 개시 전문/S : 전송/E : 마지막 전송)
		model.getSb().append("I");
		// 전문길이 4
		model.getSb().append(Telegram.numPad(35, 4));
		// 파일명 20
		model.getSb().append(Telegram.strPad(model.getFile().getName(), 20));
		// 파일크기 10
		model.getSb().append(Telegram.numPad(model.getSize(), 10));
		// 파일내용 36~5120 가변

		ByteBuf bb = Unpooled.buffer();
		bb.writeBytes(model.getSb().toString().getBytes());

		model.getSb().setLength(0);

		ctx.writeAndFlush(bb);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf b = (ByteBuf) msg;

		if (model.getPacket() == null)
			model.setPacket(ctx.alloc().buffer());

		model.getPacket().writeBytes(b);
		b.release();

		process();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		clearModel(ctx);

		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		clearModel(ctx);

		log.error("exceptionCaught : ", cause);

		ctx.close();
	}

	private void process() {
		while (model.getPacket().readableBytes() >= 12) {
			byte[] bt = new byte[12];

			model.getPacket().readBytes(bt, 0, bt.length);

			switch ((char) bt[0]) {
			// 전송
			case 'S':

				break;
			// 전송완료
			default:

				break;
			}

			model.getPacket().release();
		}
	}

	private void clearModel(ChannelHandlerContext ctx) {
		if (model.getPacket() != null) {
			while (model.getPacket().refCnt() > 0)
				model.getPacket().release();
		}

		try {
			if (model.getFis() != null)
				model.getFis().close();
			if (model.getBis() != null)
				model.getBis().close();
		} catch (IOException e) {
			log.error("IOException : ", e);
		}

		model = null;
	}

}