# CIFF Design Rationale

Instead of the design we actually adopted, one might have considered an alternative design comprising of a single protobuf message (e.g., the postings lists and doc records are contained in the header).
In this case, the entire specification of CIFF would be captured in a single protobuf definition.
This design was considered and rejected because it seems to run counter to [best practices suggested by Google](https://developers.google.com/protocol-buffers/docs/techniques): individual protobuf messages shouldn't be that large.
Furthermore, Google's automatically-generated code bindings appear to manipulate individual protobuf messages in memory, which would not be practical in our use case for large collections if the entire index were a single protobuf message.

Yet another alternative was a separate file for each type of protobuf message, and thus CIFF would be a bundle of files.
Ultimately, we decided on a single-file approach.

Additional discussion is captured in this [pull request](https://github.com/osirrc/ciff/pull/6).
