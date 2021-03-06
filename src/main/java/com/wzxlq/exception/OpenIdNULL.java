package com.wzxlq.exception;

import lombok.Data;

/**
 * @author 王照轩
 * @date 2020/4/27 - 10:19
 */
@Data
public class OpenIdNULL extends RuntimeException{
    private Integer code;

  public OpenIdNULL(String message, Integer code) {
    super(message);
    this.code = code;
  }
}
