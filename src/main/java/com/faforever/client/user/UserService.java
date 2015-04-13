package com.faforever.client.user;

import com.faforever.client.util.Callback;

public interface UserService {

  boolean isLoggedIn();

  void login(String username, String password, boolean autoLogin, Callback<Void> callback);

  String getUsername();

  String getPassword();

  String getClan();

  String getCountry();

  Float getDeviation();

  Float getMean();
}