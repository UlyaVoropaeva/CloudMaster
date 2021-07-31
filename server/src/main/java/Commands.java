public enum Commands {
        SERVER_NOTIFICATION_CLIENT_CONNECTED,//оповещение от сервера, что клиент подключился
        REQUEST_SERVER_DISCONNECT,//запрос на сервер на отсоединение пользователя от сервера
        SERVER_RESPONSE_DISCONNECT_OK,//ответ сервера, что отсоединение прошло успешно
        SERVER_RESPONSE_DISCONNECT_ERROR,//ответ сервера, что при отсоединении произошла ошибка

        REQUEST_SERVER_REGISTRATION,//запрос на сервер на регистрация пользователя с таким логином и паролем
        SERVER_RESPONSE_REGISTRATION_OK,//ответ сервера, что регистрация прошла успешно
        SERVER_RESPONSE_REGISTRATION_ERROR,//ответ сервера, что при регистрации произошла ошибка

        REQUEST_SERVER_AUTH,//запрос на сервер на авторизацию пользователя с таким логином и паролем
        SERVER_RESPONSE_AUTH_OK,//ответ сервера, что авторизация прошла успешно
        SERVER_RESPONSE_AUTH_ERROR,//ответ сервера, что при авторизации произошла ошибка

    }