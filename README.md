## Getting Started

Welcome to the VS Code Java world. Here is a guideline to help you get started to write Java code in Visual Studio Code.

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

> If you want to customize the folder structure, open `.vscode/settings.json` and update the related settings there.

## Dependency Management

The `JAVA PROJECTS` view allows you to manage your dependencies. More details can be found [here](https://github.com/microsoft/vscode-java-dependency#manage-dependencies).

## Requirements
> Request/Reply Format
> Big Endian byte ordering 
> Two different invocation semantics: at-least-once and at-most-once
- Timeouts, filtering duplicate request messages, and maintaining histories
> Simulate the loss of request/reply messages
> Design experiments to compare between the two semantics

## Tasks
# Reading of File
- Input: Filename, Offset in bytes, Bytes to read.
- Return: Bytes read starting from designated offset
- Error Handling: File does not exist, offset exceeds length.
- Error Return: Error message 
- Example: 
- If 2 bytes are read from the file content “abcd” at offset 1, the service returns “bc”.

# Writing to File
- Input: Filename, Offset in bytes, Bytes to write.
- Return: An acknowledgement
- Error Handling: File does not exist, offset exceeds length.
- Error Return: Error message 
- Example: 
- If “b” is inserted into the file content “acd” at offset 1, the updated file content becomes “abcd”.

# Monitor File
- Input: Filename, Duration in seconds
- Return: File content on every update 
- Error Handling: File does not exist
- Error Return: Error message 
- Example: 
- If File set to be monitored, any update to the file on the Server by other Clients will send the updated content to the Client monitoring that file.

## Additional Tasks
# Idempotent task
List all files
Delete file

# Non-Idempotent task
Copy file from local to remote

# Client Cache Files
- No need for replacemente algo
- One-copy update
- Implement args, t, to determine freshness interval