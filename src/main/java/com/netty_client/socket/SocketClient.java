package com.netty_client.socket;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class SocketClient implements Runnable {

	public static final Logger log = LoggerFactory.getLogger(SocketClient.class);

	private int port;
	private String host;
	private Bootstrap b = null;
	private EventLoopGroup group = null;
	private InitHandler handlers = null;

	public SocketClient(String host, int port, String fileName, String filePath, int fileSize) {
		ArrayList<ChannelHandler> handlers = new ArrayList<ChannelHandler>();

		handlers.add(new ClientHandler(fileName, filePath, fileSize));

		this.port = port;
		this.host = host;
		this.handlers = new InitHandler(handlers);
	}

	@Override
	public void run() {
		b = new Bootstrap();
		group = new NioEventLoopGroup();

		b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000).handler(this.handlers);

		try {
			ChannelFuture cf = b.connect(this.host, this.port).sync();
			cf.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			log.error("InterruptedException : ", e);
		} finally {
			b.config().group().shutdownGracefully();
		}
	}

}