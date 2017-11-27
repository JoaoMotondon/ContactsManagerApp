# ContactsManagerApp
As stated in the [docs](https://developer.android.com/guide/topics/providers/contacts-provider.html), “the Contacts Provider is a powerful and flexible Android component that manages the device's central repository of data about people”.

Data for each contact is stored in multiple tables (e.g.: Contacts, Data, RawContacts, StructureName, etc), so, in order to access the whole data for a single person, you may need to make multiple queries. This is why Contacts Provider API includes an extensive set of contract classes and interfaces that makes our lives a little easy.

The goal of this project is to demonstrate how to manage contacts data by using Contacts Provider component. It shows how to manually query for contacts as well as create, delete or change them. 

![Demo](https://user-images.githubusercontent.com/4574670/33289841-f3608e72-d3a7-11e7-8d4e-19a07984b044.gif)

The main class is [ContactsManager.java](https://github.com/JoaoMotondon/ContactsManagerApp/blob/master/app/src/main/java/com/motondon/contactsmanagerapp/provider/ContactsManager.java) class, which implements the whole process of accessing Contacts Provider. 

Basically, within this app, you can add/change the following attributes for a contact:
  - First Name
  - Last Name
  - Photo (from camera or gallery)
  - Phone (multiples per contact)
  - E-mail (multiples per contact)
  - Address (multiples per contact)
  - Organization Name
  - Organization Title
  - Custom Phone Type
  - Custom email type
  - Custom Address Type

You can also dial to a contact phone, browse to a contact address (by using Maps)  or send an email to a contact.

## Some notes about the app you may find useful
  - Prior to add a photo to the Content Provider, it takes the following actions (otherwise we would get a TransactionTooLargeException):
      - Request Contact Provider the max image size
      - Scale the image to up to the max image size
      - Compress the image
  - It uses ContentProviderOperation (CTO) class that allows us to create batch operations in the Content Provider. Since this app adds entries in different tables (and different mimetypes), it uses CTO to add each operation individually (but they run at once as a single transaction). It is also possible to create yield points in a batch process. When doing so, all entries between a yield point will act as a single transaction (but not used on this project).
  - When adding/modifying a contact organization, it will always be as of type WORK.
  - It is not possible to remove a contact photo.
  - Although it is possible to have multiples raw contacts entries for a contact (e.g.: when an user has one account for Google, another one for WhatsApp, etc), this app assumes only a single RawContact per one Contact. If you want to support multiples accounts, you will need to change this app to fit your needs.
  - When adding/removing/modifying a contact, if something goes wrong, there is no warning to the user (e.g.: a Toast, a Snackbar, etc). This could be easily fixed by throwing a business exception when catching such errors in the ContactsManager class and present to the user a friendly message.

If you want to know more about my projects, please, visit [my blog](http://androidahead.com/).

# License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details


