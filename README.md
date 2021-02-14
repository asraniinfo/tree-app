# Tree Service

## Contents

- [Introduction](#Introduction)
- [Installation](#Installation)
- [Usage](#Usage)

## Introduction

This is a REST API service in response to below problem statement:

We in Amazing Co need to model how our company is structured so we can do awesome stuff. We have a root node (only one)
and several children nodes,each one with its own children as well. It’s a tree-based structure. Something like:

```
            root
          /   \
         a     b

```

We need two HTTP APIs that will serve the two basic operations:

1) API Get all(direct and non-direct) child nodes of a given node (the given node can be anyone in the tree
   structure). Change the parent node of a given node (the given node can be anyone in the tree structure). They need to
   answer quickly, even with tons of nodes. Also, we can’t afford to lose this information, so persistence is required.
   Each node should have the following info:
   a) node identification b) who is the parent node c) who is the root node d) the height of the node. In the above
   example,height(root) = 0 and height(a) == 1.
2) We can only have docker and docker-compose on our machines, so your server needs to be run using them.
3) Make a simple UI for this challenge. If you feel adventurous you can use our UI components (
   ​http://ui.tradeshift.com​ )

## Installation

This service requires Docker and docker-compose to be installed on your system. In addition, HTTP port 8084 must be
available on the host.

```bash
$ cd tree-app
$ docker-compose up
```

This will first create two PostgreSQL containers (one for the unit tests and one for the API)
and then run the unit tests. After the unit tests run successfully, the API container starts and exposes the Spring Boot
service on the host port 8084.

## Usage

The service comes pre-loaded with the following sample data:

```
            root
          /   \
         a     b
       /  \
      c    d
    / \\
   e  f g
```

#### Get Descendants

Use the following command to return all children of any node.

```
$ curl http://localhost:8084/api/v1/node/a/children
[
  {
    "id": "c",
    "parentId": "a",
    "rootId": "root",
    "height": 2
  },
  {
    "id": "d",
    "parentId": "a",
    "rootId": "root",
    "height": 2
  },
  {
    "id": "e",
    "parentId": "c",
    "rootId": "root",
    "height": 3
  },
  {
    "id": "f",
    "parentId": "c",
    "rootId": "root",
    "height": 3
  },
  {
    "id": "g",
    "parentId": "c",
    "rootId": "root",
    "height": 3
  }
]
``` 

#### Move Node

Use the following command to move any node (and its subtree) to a new parent node:

```
$ curl http://localhost:8084/api/v1/node/c/moveNode/b
```

This moves node 4 from its previous parent node to node 3 as its new parent node.

We can then verify that the node was moved by checking the children. We first check the descendants of the previous
parent node to ensure that the child node was removed:

```
$ curl http://localhost:8084/api/v1/node/a/children
[
  {
    "id": "d",
    "parentId": "a",
    "rootId": "root",
    "height": 2
  }
]
```

Now, we verify that the new parent has the child node and its subtree:

```
$ curl http://localhost:8084/api/v1/node/b/children
[
  {
    "id": "c",
    "parentId": "b",
    "rootId": "root",
    "height": 2
  },
  {
    "id": "e",
    "parentId": "c",
    "rootId": "root",
    "height": 3
  },
  {
    "id": "f",
    "parentId": "c",
    "rootId": "root",
    "height": 3
  },
  {
    "id": "g",
    "parentId": "c",
    "rootId": "root",
    "height": 3
  }
]
```

# Note:

for verification/testing purpose have left endpoints to create and fetch nodes.

## Error Handling

If we try to move a node to one of its children, we get an HTTP 508 (Loop Detected) error:

```
$ curl -i http://localhost:8084/api/v1/node/c/moveNode/e
HTTP/1.1 508
You may not move a node to one of its child
```

If we try to move a node to a node that does not exist, we get an HTTP 404 (Not Found) error:

```
$ curl -i http://localhost:8084/api/v1/node/c/moveNode/ax
HTTP/1.1 404
The specified node ax does not exist
```

