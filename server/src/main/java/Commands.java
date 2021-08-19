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

        REQUEST_SERVER_UPLOAD_ITEM,//запрос на сервер загрузить(сохранить) объект элемента списка
        SERVER_RESPONSE_UPLOAD_ITEM_OK,//ответ сервера, что объект успешно загружен(сохранен)
        SERVER_RESPONSE_UPLOAD_ITEM_ERROR,//ответ сервера, что при загрузке(сохранении) объекта произошла ошибка

        REQUEST_SERVER_UPLOAD_FILE_FRAG,//запрос на сервер загрузить(сохранить) фрагмент файла
        SERVER_RESPONSE_UPLOAD_FILE_FRAG_OK,//ответ сервера, что фрагмент файла успешно загружен(сохранен)
        SERVER_RESPONSE_UPLOAD_FILE_FRAG_ERROR,//ответ сервера, что при загрузке(сохранении) фрагмент файла произошла ошибка
        SERVER_RESPONSE_UPLOAD_FILE_FRAGS_OK,//ответ сервера, что сборка файла из загруженных фрагментов прошла успешно
        SERVER_RESPONSE_UPLOAD_FILE_FRAGS_ERROR,

        //запрос на сервер скачать объект элемента списка(пока только файл)
        REQUEST_SERVER_DOWNLOAD_ITEM,
        //ответ сервера с объектом элемента списка(пока только файл), если нет ошибок
        SERVER_RESPONSE_DOWNLOAD_ITEM_OK,
        //ответ сервера, что при скачивании объекта элемента списка(пока только файл) произошла ошибка
        SERVER_RESPONSE_DOWNLOAD_ITEM_ERROR,
        //обрабатываем полученный от сервера ответ на запрос на скачивание с фрагментом файла из облачного хранилища
        SERVER_RESPONSE_DOWNLOAD_FILE_FRAG_OK,
        CLIENT_RESPONSE_DOWNLOAD_FILE_FRAG_ERROR,
        CLIENT_RESPONSE_DOWNLOAD_FILE_FRAG_OK,
}