package com.netty_client.socket;

import java.util.ArrayList;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitHandler extends ChannelInitializer<SocketChannel> {
	private ArrayList<ChannelHandler> hansdlers = null;

	public InitHandler(ArrayList<ChannelHandler> hansdlers) {
		this.hansdlers = hansdlers;
	}

	@Override
	protected void initChannel(SocketChannel sc) throws Exception {
		ChannelPipeline pipe = sc.pipeline();

		for (ChannelHandler handler : hansdlers)
			pipe.addLast(handler);
	}

}