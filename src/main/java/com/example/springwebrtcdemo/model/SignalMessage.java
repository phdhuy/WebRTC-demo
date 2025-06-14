package com.example.springwebrtcdemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignalMessage {

  private String type;

  private String sender;

  private String receiver;

  private Object data;
}
