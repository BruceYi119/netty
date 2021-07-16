package com.netty_client.component;

import org.springframework.stereotype.Component;

@Component
public class Telegram {

	public static String numPad(int n, int len) {
		return String.format("%0" + len + "d", n);
	}

	public static String numPad(long n, int len) {
		return String.format("%0" + len + "d", n);
	}

	public static String strPad(String str, int n) {
		return String.format("%-" + n + "s", str);
	}

}