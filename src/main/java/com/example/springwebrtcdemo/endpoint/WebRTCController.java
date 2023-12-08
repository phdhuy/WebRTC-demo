package com.example.springwebrtcdemo.endpoint;

import com.example.springwebrtcdemo.constant.CommonConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/webrtc")
public class WebRTCController {

  @PostMapping("/call/{conversationId}")
  public ResponseEntity<String> signalingSocketHandler(@PathVariable UUID conversationId) {
    String res = CommonConstant.WEB_SOCKET_URL + conversationId;
    return ResponseEntity.ok(res);
  }
}
