package com.netty_client.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.netty_client.socket.SocketClient;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class MainController {

	private Environment env;

	@GetMapping("/")
	public String main() {
		return "redirect:/file/send";
	}

	@GetMapping("/file/send")
	public String sendForm() {
		return "file/send";
	}

	@PostMapping("/file/send")
	@ResponseBody
	@SuppressWarnings("finally")
	public Object send(@RequestParam("files") MultipartFile[] files) throws IOException {
		int port = Integer.parseInt(env.getProperty("custom.socket.client.port"));
		String ip = env.getProperty("custom.socket.client.id");
		Map<String, String> json = new HashMap<String, String>();
		ArrayList<Thread> threads = new ArrayList<Thread>();

		json.put("result", "fail");

		Path path = Paths.get(env.getProperty("custom.file.upload.path"));

		if (!path.toFile().exists())
			path.toFile().mkdirs();

		try {
			for (MultipartFile f : files) {
				if (f.getSize() > 0) {
					File file = new File(path.toString(), f.getOriginalFilename());

					f.transferTo(file);

					threads.add(new Thread(
							new SocketClient(ip, port, file.getName(), file.getPath(), (int) file.length())));
				}
			}

			for (Thread t : threads)
				t.start();

			json.put("result", "success");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return json;
		}
	}

}