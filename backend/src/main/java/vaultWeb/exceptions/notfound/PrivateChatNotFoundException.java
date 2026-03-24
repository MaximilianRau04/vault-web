package vaultWeb.exceptions.notfound;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PrivateChatNotFoundException extends RuntimeException {
  public PrivateChatNotFoundException(String message) {
    super(message);
  }
}
